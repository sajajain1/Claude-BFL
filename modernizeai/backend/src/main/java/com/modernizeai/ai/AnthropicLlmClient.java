package com.modernizeai.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.NotFoundException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.modernizeai.analysis.SecretRedactor;
import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.exception.AiResponseInvalidException;
import com.modernizeai.exception.AiUnavailableException;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * {@link LlmClient} backed by the official Anthropic Java SDK. The API key is read from the
 * ANTHROPIC_API_KEY environment variable, never logged and never returned to clients.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final ModernizeAiProperties.Ai config;
    private final String apiKey;
    private final String model;
    private volatile AnthropicClient client;

    public AnthropicLlmClient(ModernizeAiProperties properties, Environment env) {
        this.config = properties.ai();
        String key = env.getProperty("ANTHROPIC_API_KEY");
        this.apiKey = key == null || key.isBlank() ? null : key.trim();
        String modelOverride = env.getProperty("ANTHROPIC_MODEL");
        this.model = modelOverride == null || modelOverride.isBlank() ? config.model() : modelOverride.trim();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null;
    }

    @Override
    public String modelName() {
        return model;
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.builder()
                            .apiKey(apiKey)
                            .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                            .maxRetries(1)
                            .build();
                }
            }
        }
        return client;
    }

    @Override
    public LlmResult complete(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        if (!isConfigured()) {
            throw new AiUnavailableException("AI_NOT_CONFIGURED", "AI explanations require an Anthropic API key. "
                    + "Set the ANTHROPIC_API_KEY environment variable and restart the backend.");
        }
        MessageCreateParams params = buildParams(systemPrompt, userPrompt, jsonSchema);

        long start = System.nanoTime();
        Message message;
        try {
            message = client().messages().create(params);
        } catch (UnauthorizedException | PermissionDeniedException e) {
            throw new AiUnavailableException("AI_AUTH_FAILED", "The Anthropic API rejected the API key. Check ANTHROPIC_API_KEY.", e);
        } catch (RateLimitException e) {
            throw new AiUnavailableException("AI_RATE_LIMITED", "The Anthropic API rate limit was reached. Try again shortly.", e);
        } catch (NotFoundException e) {
            throw new AiUnavailableException("AI_MODEL_NOT_FOUND", "The configured model '" + model + "' was not found. Check ANTHROPIC_MODEL.", e);
        } catch (BadRequestException e) {
            // Includes 'credit balance too low'; the API's message is useful and contains no secrets, but redact anyway.
            throw new AiUnavailableException("AI_REQUEST_REJECTED", "The Anthropic API rejected the request: " + safe(e.getMessage()), e);
        } catch (AnthropicServiceException e) {
            throw new AiUnavailableException("AI_UPSTREAM_ERROR", "The Anthropic API returned an error (HTTP " + e.statusCode() + ").", e);
        } catch (AnthropicIoException e) {
            throw new AiUnavailableException("AI_UNREACHABLE", "Could not reach the Anthropic API (network error or timeout after "
                    + config.timeoutSeconds() + "s).", e);
        }

        String stop = message.stopReason().map(StopReason::toString).orElse("unknown");
        log.info("Claude call complete model={} stopReason={} durationMs={}", message.model(), stop, (System.nanoTime() - start) / 1_000_000);
        if (message.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            throw new AiUnavailableException("AI_REFUSED", "Claude declined to answer this request. The deterministic findings are unaffected.");
        }
        if (message.stopReason().filter(StopReason.MAX_TOKENS::equals).isPresent()) {
            throw new AiResponseInvalidException("Claude's answer was cut off (max_tokens). Try explaining fewer findings at once.");
        }
        String text = message.content().stream()
                .flatMap(b -> b.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining());
        return new LlmResult(text, message.model().toString(), stop);
    }

    /** Builds the request. Package-private so tests can inspect it without network access. */
    MessageCreateParams buildParams(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        JsonOutputFormat.Schema.Builder schema = JsonOutputFormat.Schema.builder();
        jsonSchema.forEach((k, v) -> schema.putAdditionalProperty(k, JsonValue.from(v)));

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(config.maxOutputTokens())
                .system(systemPrompt)
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.MEDIUM)
                        .format(JsonOutputFormat.builder().schema(schema.build()).build())
                        .build())
                .addUserMessage(userPrompt);
        if (config.serverSideFallback()) {
            // Opt in to Anthropic's server-side refusal fallback (routes a declined request to a fallback model).
            params.putAdditionalHeader("anthropic-beta", FALLBACK_BETA);
            params.putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        return params.build();
    }

    private static String safe(String msg) {
        if (msg == null) return "no details";
        String s = SecretRedactor.redact(msg.replaceAll("[\\r\\n]+", " "));
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    @PreDestroy
    void close() {
        if (client != null) client.close();
    }
}
