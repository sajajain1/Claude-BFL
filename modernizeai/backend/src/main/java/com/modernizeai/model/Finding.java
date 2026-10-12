package com.modernizeai.model;

import java.util.List;

/**
 * A single deterministic finding. {@code snippet} holds a few lines of redacted context around
 * {@code line}; it is the only part of the uploaded source retained after analysis.
 */
public record Finding(
        String id,
        String ruleId,
        String title,
        Category category,
        Severity severity,
        Certainty certainty,
        String filePath,
        Integer line,
        String evidence,
        String snippet,
        String explanation,
        String suggestedNextStep,
        List<String> references) {

    public Finding withId(String newId) {
        return new Finding(newId, ruleId, title, category, severity, certainty, filePath, line, evidence,
                snippet, explanation, suggestedNextStep, references);
    }
}
