package com.modernizeai.model;

import java.util.List;

/**
 * Facts extracted from the project. Every "detected" value records where it came from so the UI
 * can show evidence rather than assertions.
 */
public record ProjectSummary(
        String groupId,
        String artifactId,
        String version,
        String javaVersion,
        String javaVersionSource,
        String springBootVersion,
        String springBootVersionSource,
        int javaFileCount,
        int filesScanned,
        int filesSkipped,
        List<String> parseWarnings,
        List<DependencyInfo> keyDependencies,
        int totalDependencies) {}
