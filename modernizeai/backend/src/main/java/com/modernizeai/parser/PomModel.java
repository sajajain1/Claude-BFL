package com.modernizeai.parser;

import com.modernizeai.model.DependencyInfo;
import java.util.List;
import java.util.Map;

/**
 * The subset of a Maven POM that the migration rules need. Values are as declared in this single
 * file; inherited values from remote parents are NOT resolved (the server never runs Maven).
 */
public record PomModel(
        String path,
        String groupId,
        String artifactId,
        String version,
        String packaging,
        Parent parent,
        Map<String, Property> properties,
        List<DependencyInfo> dependencies,
        List<DependencyInfo> managedDependencies,
        List<Plugin> plugins,
        List<String> modules) {

    public record Parent(String groupId, String artifactId, String version, Integer line) {}

    public record Property(String name, String value, Integer line) {}

    /** A build plugin with the compiler-relevant configuration flattened. */
    public record Plugin(String groupId, String artifactId, String version, Integer line,
                         Map<String, Property> configuration) {}

    public boolean hasSpringBootParent() {
        return parent != null && "org.springframework.boot".equals(parent.groupId())
                && "spring-boot-starter-parent".equals(parent.artifactId());
    }

    public String property(String name) {
        Property p = properties.get(name);
        return p == null ? null : p.value();
    }
}
