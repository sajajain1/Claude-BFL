package com.modernizeai.ai;

import java.util.Map;

/**
 * Narrow seam between the application and the LLM provider, so the explanation logic can be
 * tested with mocks and without network access or API costs.
 */
public interface LlmClient {

    /** True when credentials are present. Does not make a network call. */
    boolean isConfigured();

    String modelName();

    /**
     * Sends one request and returns the raw text the model produced.
     *
     * @param jsonSchema JSON Schema the response must follow (enforced by the API's structured
     *                   outputs and re-validated locally by {@link AiResponseParser})
     * @throws com.modernizeai.exception.AiUnavailableException for configuration, network, quota or refusal errors
     */
    LlmResult complete(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema);

    record LlmResult(String text, String model, String stopReason) {}
}
