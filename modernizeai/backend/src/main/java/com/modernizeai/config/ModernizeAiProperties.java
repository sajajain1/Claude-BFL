package com.modernizeai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Central, validated configuration. All limits are enforced server-side regardless of what the
 * frontend does.
 */
@Validated
@ConfigurationProperties(prefix = "modernizeai")
public record ModernizeAiProperties(@Valid Upload upload, @Valid Analysis analysis, @Valid Ai ai, List<String> corsAllowedOrigins) {

    public ModernizeAiProperties {
        if (upload == null) upload = new Upload(20L * 1024 * 1024, 5000, 200L * 1024 * 1024, 2L * 1024 * 1024, 100);
        if (analysis == null) analysis = new Analysis(17, 50, 30);
        if (ai == null) ai = new Ai("claude-opus-5-5", 90, 8000, 5, 1500, true);
        if (corsAllowedOrigins == null) corsAllowedOrigins = List.of("http://localhost:5173");
    }

    /**
     * @param maxZipBytes           maximum size of the uploaded archive
     * @param maxEntries            maximum number of entries (files + directories) in the archive
     * @param maxTotalUncompressedBytes maximum total bytes read out of the archive
     * @param maxFileBytes          maximum bytes for a single analyzed file (larger files are skipped)
     * @param maxCompressionRatio   per-entry ratio above which the archive is treated as a zip bomb
     */
    public record Upload(
            @Min(1024) long maxZipBytes,
            @Min(1) @Max(100_000) int maxEntries,
            @Min(1024) long maxTotalUncompressedBytes,
            @Min(1024) long maxFileBytes,
            @Min(10) int maxCompressionRatio) {}

    /**
     * @param defaultTargetJavaVersion Java version used when the request does not specify one
     * @param maxStoredAnalyses        in-memory LRU size for results of the current session
     * @param resultTtlMinutes         results older than this are evicted
     */
    public record Analysis(
            @Min(11) @Max(25) int defaultTargetJavaVersion,
            @Min(1) @Max(1000) int maxStoredAnalyses,
            @Min(1) @Max(1440) int resultTtlMinutes) {}

    /**
     * @param model                  Claude model id (override with ANTHROPIC_MODEL)
     * @param timeoutSeconds         HTTP timeout for a single Claude request
     * @param maxOutputTokens        max_tokens for the explanation request
     * @param maxFindingsPerRequest  hard cap on findings sent to Claude in one call
     * @param maxSnippetChars        hard cap on characters of source sent per finding
     * @param serverSideFallback     opt in to Anthropic's server-side refusal fallback
     */
    public record Ai(
            @NotBlank String model,
            @Min(5) @Max(600) int timeoutSeconds,
            @Min(512) @Max(64000) int maxOutputTokens,
            @Min(1) @Max(25) int maxFindingsPerRequest,
            @Min(200) @Max(10_000) int maxSnippetChars,
            boolean serverSideFallback) {}
}
