package com.modernizeai.model;

import java.util.List;

/** A validated explanation produced by Claude for one finding. */
public record AiExplanation(
        String findingId,
        String whyItMayOccur,
        String minimalFix,
        String compatibilityNotes,
        List<String> missingContext,
        String confidence) {}
