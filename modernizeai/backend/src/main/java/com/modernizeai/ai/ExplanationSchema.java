package com.modernizeai.ai;

import java.util.List;
import java.util.Map;

/** JSON Schema for Claude's answer. Kept to keywords supported by structured outputs. */
public final class ExplanationSchema {

    public static final List<String> CONFIDENCE = List.of("low", "medium", "high");

    private ExplanationSchema() {}

    public static Map<String, Object> schema() {
        Map<String, Object> item = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("findingId", "whyItMayOccur", "minimalFix", "compatibilityNotes", "missingContext", "confidence"),
                "properties", Map.of(
                        "findingId", Map.of("type", "string", "description", "Exactly one of the finding ids supplied"),
                        "whyItMayOccur", Map.of("type", "string"),
                        "minimalFix", Map.of("type", "string"),
                        "compatibilityNotes", Map.of("type", "string"),
                        "missingContext", Map.of("type", "array", "items", Map.of("type", "string")),
                        "confidence", Map.of("type", "string", "enum", CONFIDENCE)));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("explanations"),
                "properties", Map.of("explanations", Map.of("type", "array", "items", item)));
    }
}
