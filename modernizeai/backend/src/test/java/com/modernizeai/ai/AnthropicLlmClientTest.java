package com.modernizeai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.models.messages.MessageCreateParams;
import com.modernizeai.exception.AiUnavailableException;
import com.modernizeai.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** No network calls: verifies configuration handling and the request we would send. */
class AnthropicLlmClientTest {

    @Test
    void notConfiguredWithoutKey() {
        AnthropicLlmClient client = new AnthropicLlmClient(TestSupport.defaultProperties(), new MockEnvironment());
        assertThat(client.isConfigured()).isFalse();
        assertThat(client.modelName()).isEqualTo("claude-opus-5-5");
        assertThatThrownBy(() -> client.complete("s", "u", ExplanationSchema.schema()))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    @Test
    void blankKeyCountsAsMissingAndModelCanBeOverridden() {
        MockEnvironment env = new MockEnvironment().withProperty("ANTHROPIC_API_KEY", "  ").withProperty("ANTHROPIC_MODEL", "claude-haiku-5-5");
        AnthropicLlmClient client = new AnthropicLlmClient(TestSupport.defaultProperties(), env);
        assertThat(client.isConfigured()).isFalse();
        assertThat(client.modelName()).isEqualTo("claude-haiku-5-5");
    }

    @Test
    void buildsStructuredOutputRequestWithFallbackOptIn() {
        MockEnvironment env = new MockEnvironment().withProperty("ANTHROPIC_API_KEY", "sk-ant-test-not-real");
        AnthropicLlmClient client = new AnthropicLlmClient(TestSupport.defaultProperties(), env);
        assertThat(client.isConfigured()).isTrue();

        MessageCreateParams params = client.buildParams("system prompt", "user prompt", ExplanationSchema.schema());
        assertThat(params.model().toString()).isEqualTo("claude-opus-5-5");
        assertThat(params.maxTokens()).isEqualTo(8000);
        String body = params._body().toString() + params._additionalBodyProperties();
        assertThat(body).contains("explanations", "findingId", "additionalProperties");
        assertThat(params._additionalBodyProperties()).containsKey("fallbacks");
        assertThat(params._headers().values("anthropic-beta")).containsExactly(AnthropicLlmClient.FALLBACK_BETA);
        assertThat(body).doesNotContain("sk-ant-test-not-real");
    }
}
