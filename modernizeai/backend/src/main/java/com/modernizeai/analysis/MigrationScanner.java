package com.modernizeai.analysis;

import com.modernizeai.model.Finding;
import com.modernizeai.parser.PomModel;
import com.modernizeai.parser.PomParseException;
import com.modernizeai.parser.PomParser;
import com.modernizeai.parser.ProjectFiles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Builds the {@link ProjectContext} and runs every registered {@link MigrationRule}. */
@Component
public class MigrationScanner {

    private static final Logger log = LoggerFactory.getLogger(MigrationScanner.class);

    private final PomParser pomParser;
    private final JavaSourceParser javaParser;
    private final VersionDetector versionDetector;
    private final List<MigrationRule> rules;

    public MigrationScanner(PomParser pomParser, JavaSourceParser javaParser, VersionDetector versionDetector, List<MigrationRule> rules) {
        this.pomParser = pomParser;
        this.javaParser = javaParser;
        this.versionDetector = versionDetector;
        this.rules = rules;
    }

    public record ScanResult(ProjectContext context, List<Finding> findings, List<String> warnings) {}

    public ScanResult scan(ProjectFiles files, int targetJavaVersion) {
        List<String> warnings = new ArrayList<>(files.warnings());
        List<PomModel> poms = new ArrayList<>();
        Map<String, ProjectContext.PomParseError> pomErrors = new LinkedHashMap<>();
        for (String path : files.pomPaths()) {
            try {
                poms.add(pomParser.parse(path, files.get(path).orElse("")));
            } catch (PomParseException e) {
                pomErrors.put(path, new ProjectContext.PomParseError(e.getMessage(), e.line()));
                warnings.add(e.getMessage());
            }
        }
        Optional<String> rootPath = files.rootPomPath();
        Optional<PomModel> root = rootPath.flatMap(p -> poms.stream().filter(m -> m.path().equals(p)).findFirst());

        List<JavaSource> sources = new ArrayList<>();
        for (String path : files.pathsEndingWith(".java")) {
            JavaSource src = javaParser.parse(path, files.get(path).orElse(""));
            if (!src.parsed()) warnings.add("Could not fully parse " + path + " (" + src.parseError() + "); used text matching instead.");
            sources.add(src);
        }

        Optional<DetectedValue> boot = root.flatMap(versionDetector::springBootVersion);
        if (boot.isEmpty()) {
            boot = poms.stream().map(versionDetector::springBootVersion).flatMap(Optional::stream).findFirst();
        }
        final Optional<DetectedValue> bootFinal = boot;
        Optional<DetectedValue> java = root.flatMap(p -> versionDetector.javaVersion(p, bootFinal));
        if (java.isEmpty()) {
            java = poms.stream().map(p -> versionDetector.javaVersion(p, bootFinal)).flatMap(Optional::stream).findFirst();
        }

        ProjectContext ctx = new ProjectContext(files, poms, root, pomErrors, sources, boot, java, targetJavaVersion);

        List<Finding> findings = new ArrayList<>();
        for (MigrationRule rule : rules) {
            try {
                findings.addAll(rule.evaluate(ctx));
            } catch (RuntimeException e) {
                // One faulty rule must not hide the results of the others.
                log.error("Rule failed rule={} error={}", rule.name(), e.getClass().getName(), e);
                warnings.add("Internal error in rule '" + rule.name() + "'; its results are missing from this report.");
            }
        }
        findings.sort(Comparator.comparing(Finding::severity)
                .thenComparing(Finding::certainty)
                .thenComparing(Finding::category)
                .thenComparing(f -> f.filePath() == null ? "" : f.filePath())
                .thenComparing(f -> f.line() == null ? 0 : f.line()));
        List<Finding> numbered = new ArrayList<>(findings.size());
        for (int i = 0; i < findings.size(); i++) {
            numbered.add(findings.get(i).withId(String.format("F-%03d", i + 1)));
        }
        return new ScanResult(ctx, numbered, warnings);
    }
}
