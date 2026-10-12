package com.modernizeai.service;

import com.modernizeai.analysis.DetectedValue;
import com.modernizeai.analysis.MigrationPlanner;
import com.modernizeai.analysis.MigrationScanner;
import com.modernizeai.analysis.ProjectContext;
import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.exception.InvalidUploadException;
import com.modernizeai.exception.NotAMavenProjectException;
import com.modernizeai.model.AnalysisReport;
import com.modernizeai.model.Category;
import com.modernizeai.model.DependencyInfo;
import com.modernizeai.model.Finding;
import com.modernizeai.model.ProjectSummary;
import com.modernizeai.model.Severity;
import com.modernizeai.parser.PomModel;
import com.modernizeai.parser.ProjectFiles;
import com.modernizeai.parser.SafeZipReader;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    public static final String DISCLAIMER = "ModernizeAI performs static, rule-based analysis without building or running your code. "
            + "Findings marked POTENTIAL are pattern matches that need human review; even VERIFIED findings describe "
            + "observed facts, not proof that a migration will fail. Review every recommendation before changing code.";

    private final SafeZipReader zipReader;
    private final MigrationScanner scanner;
    private final MigrationPlanner planner;
    private final AnalysisStore store;
    private final ModernizeAiProperties properties;

    public AnalysisService(SafeZipReader zipReader, MigrationScanner scanner, MigrationPlanner planner, AnalysisStore store,
                           ModernizeAiProperties properties) {
        this.zipReader = zipReader;
        this.scanner = scanner;
        this.planner = planner;
        this.store = store;
        this.properties = properties;
    }

    public AnalysisReport analyze(MultipartFile file, Integer targetJavaVersion) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Please choose a ZIP file to upload.");
        }
        String name = sanitizeFilename(file.getOriginalFilename());
        if (!name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new InvalidUploadException("Only .zip archives are accepted.");
        }
        if (file.getSize() > properties.upload().maxZipBytes()) {
            throw new InvalidUploadException("The uploaded file exceeds the " + (properties.upload().maxZipBytes() / (1024 * 1024)) + " MB limit.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidUploadException("The upload could not be read.");
        }
        return analyze(name, bytes, targetJavaVersion);
    }

    public AnalysisReport analyze(String filename, byte[] zipBytes, Integer targetJavaVersion) {
        int target = targetJavaVersion != null ? targetJavaVersion : properties.analysis().defaultTargetJavaVersion();
        long start = System.nanoTime();
        ProjectFiles files = zipReader.read(zipBytes);
        if (files.pomPaths().isEmpty()) {
            throw new NotAMavenProjectException("No pom.xml was found in the archive. ModernizeAI currently supports Maven projects only.");
        }
        MigrationScanner.ScanResult result = scanner.scan(files, target);
        ProjectContext ctx = result.context();
        List<Finding> findings = result.findings();

        Map<Severity, Long> bySeverity = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) bySeverity.put(s, 0L);
        findings.forEach(f -> bySeverity.merge(f.severity(), 1L, Long::sum));
        Map<Category, Long> byCategory = findings.stream()
                .collect(Collectors.groupingBy(Finding::category, () -> new EnumMap<>(Category.class), Collectors.counting()));

        AnalysisReport report = new AnalysisReport(UUID.randomUUID().toString(), Instant.now(), filename, target, "3.2.x",
                summary(ctx, files, result.warnings()), findings, bySeverity, byCategory, planner.plan(ctx, findings), DISCLAIMER);
        store.put(report);
        log.info("Analysis complete id={} files={} javaFiles={} findings={} high={} durationMs={}", report.analysisId(),
                files.all().size(), ctx.javaSources().size(), findings.size(), bySeverity.get(Severity.HIGH),
                (System.nanoTime() - start) / 1_000_000);
        return report;
    }

    private ProjectSummary summary(ProjectContext ctx, ProjectFiles files, List<String> warnings) {
        Optional<PomModel> root = ctx.rootPom();
        Function<Optional<DetectedValue>, String> src = d -> d.map(v -> v.source() + " in " + v.path()
                + (v.line() != null ? ":" + v.line() : "")).orElse(null);
        List<DependencyInfo> deps = root.map(p -> p.dependencies().stream().limit(60).toList()).orElse(List.of());
        int total = ctx.poms().stream().mapToInt(p -> p.dependencies().size()).sum();
        return new ProjectSummary(
                root.map(PomModel::groupId).orElse(null),
                root.map(PomModel::artifactId).orElse(null),
                root.map(PomModel::version).orElse(null),
                ctx.javaVersion().map(DetectedValue::value).orElse(null),
                src.apply(ctx.javaVersion()),
                ctx.springBootVersion().map(DetectedValue::value).orElse(null),
                src.apply(ctx.springBootVersion()),
                ctx.javaSources().size(),
                files.all().size(),
                files.skippedFiles(),
                warnings.stream().limit(50).toList(),
                deps,
                total);
    }

    static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "upload.zip";
        String base = original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_");
        return base.length() > 120 ? base.substring(base.length() - 120) : base;
    }
}
