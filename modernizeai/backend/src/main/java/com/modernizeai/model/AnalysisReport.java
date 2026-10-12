package com.modernizeai.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalysisReport(
        String analysisId,
        Instant createdAt,
        String originalFilename,
        int targetJavaVersion,
        String targetSpringBoot,
        ProjectSummary project,
        List<Finding> findings,
        Map<Severity, Long> countsBySeverity,
        Map<Category, Long> countsByCategory,
        List<String> recommendedSteps,
        String disclaimer) {}
