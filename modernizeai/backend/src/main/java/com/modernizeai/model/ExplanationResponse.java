package com.modernizeai.model;

import java.util.List;

/**
 * @param model  the model that produced the explanations (as reported by the API)
 * @param notice human-readable caveat shown with every AI result
 */
public record ExplanationResponse(String analysisId, String model, List<AiExplanation> explanations, String notice) {}
