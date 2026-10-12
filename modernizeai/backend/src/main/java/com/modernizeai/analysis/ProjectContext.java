package com.modernizeai.analysis;

import com.modernizeai.parser.PomModel;
import com.modernizeai.parser.ProjectFiles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the rules can see. Built once per analysis and discarded afterwards.
 *
 * @param pomErrors path -> human-readable parse error for POMs that could not be parsed
 */
public record ProjectContext(
        ProjectFiles files,
        List<PomModel> poms,
        Optional<PomModel> rootPom,
        Map<String, PomParseError> pomErrors,
        List<JavaSource> javaSources,
        Optional<DetectedValue> springBootVersion,
        Optional<DetectedValue> javaVersion,
        int targetJavaVersion) {

    public record PomParseError(String message, Integer line) {}

    public Integer bootMajor() {
        return springBootVersion.map(v -> VersionUtils.major(v.value())).orElse(null);
    }

    /** True when Spring Boot dependencies are present at all (even if the version is unknown). */
    public boolean usesSpringBoot() {
        return springBootVersion.isPresent() || poms.stream().anyMatch(p ->
                p.dependencies().stream().anyMatch(d -> "org.springframework.boot".equals(d.groupId())));
    }

    public String content(String path) {
        return files.get(path).orElse(null);
    }
}
