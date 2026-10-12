package com.modernizeai.model;

/**
 * A dependency as declared in the POM.
 *
 * @param declaredVersion version text exactly as written (may contain ${...})
 * @param resolvedVersion version after resolving properties from this POM only, or null
 * @param line            line of the &lt;dependency&gt; element in pom.xml, or null
 * @param managed         true if declared under &lt;dependencyManagement&gt;
 */
public record DependencyInfo(
        String groupId,
        String artifactId,
        String declaredVersion,
        String resolvedVersion,
        String scope,
        String type,
        Integer line,
        boolean managed) {

    public String coordinates() {
        return groupId + ":" + artifactId;
    }
}
