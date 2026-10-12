package com.modernizeai.analysis;

import com.modernizeai.model.DependencyInfo;
import com.modernizeai.parser.PomModel;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Detects Java and Spring Boot versions from a POM, recording where each value came from. */
@Component
public class VersionDetector {

    public Optional<DetectedValue> springBootVersion(PomModel pom) {
        if (pom.hasSpringBootParent() && pom.parent().version() != null) {
            String v = resolve(pom, pom.parent().version());
            return Optional.of(new DetectedValue(v, pom.parent().version(),
                    "<parent> spring-boot-starter-parent", pom.path(), pom.parent().line(), false));
        }
        for (DependencyInfo d : pom.managedDependencies()) {
            if ("org.springframework.boot".equals(d.groupId()) && "spring-boot-dependencies".equals(d.artifactId())
                    && d.declaredVersion() != null) {
                return Optional.of(new DetectedValue(d.resolvedVersion() != null ? d.resolvedVersion() : d.declaredVersion(),
                        d.declaredVersion(), "spring-boot-dependencies BOM import", pom.path(), d.line(), false));
            }
        }
        for (PomModel.Plugin p : pom.plugins()) {
            if ("org.springframework.boot".equals(p.groupId()) && "spring-boot-maven-plugin".equals(p.artifactId()) && p.version() != null) {
                return Optional.of(new DetectedValue(p.version(), p.version(), "spring-boot-maven-plugin version",
                        pom.path(), p.line(), false));
            }
        }
        for (String key : new String[] {"spring-boot.version", "spring.boot.version", "springboot.version"}) {
            PomModel.Property prop = pom.properties().get(key);
            if (prop != null && prop.value() != null) {
                return Optional.of(new DetectedValue(resolve(pom, prop.value()), prop.value(), "property <" + key + ">",
                        pom.path(), prop.line(), false));
            }
        }
        for (DependencyInfo d : pom.dependencies()) {
            if ("org.springframework.boot".equals(d.groupId()) && d.resolvedVersion() != null) {
                return Optional.of(new DetectedValue(d.resolvedVersion(), d.declaredVersion(),
                        "explicit version of " + d.artifactId(), pom.path(), d.line(), false));
            }
        }
        return Optional.empty();
    }

    /**
     * Precedence mirrors Maven: compiler plugin configuration overrides the maven.compiler.*
     * properties, and Spring Boot's parent maps java.version onto them.
     */
    public Optional<DetectedValue> javaVersion(PomModel pom, Optional<DetectedValue> bootVersion) {
        for (PomModel.Plugin p : pom.plugins()) {
            if ("maven-compiler-plugin".equals(p.artifactId())) {
                for (String key : new String[] {"release", "target", "source"}) {
                    PomModel.Property c = p.configuration().get(key);
                    if (c != null && VersionUtils.javaMajor(c.value()) != null) {
                        return Optional.of(new DetectedValue(String.valueOf(VersionUtils.javaMajor(c.value())), c.value(),
                                "maven-compiler-plugin <" + key + ">", pom.path(), c.line(), false));
                    }
                }
            }
        }
        for (String key : new String[] {"maven.compiler.release", "maven.compiler.target", "maven.compiler.source", "java.version"}) {
            PomModel.Property prop = pom.properties().get(key);
            if (prop != null) {
                String resolved = resolve(pom, prop.value());
                Integer major = VersionUtils.javaMajor(resolved);
                if (major != null) {
                    return Optional.of(new DetectedValue(String.valueOf(major), prop.value(), "property <" + key + ">",
                            pom.path(), prop.line(), false));
                }
            }
        }
        if (pom.hasSpringBootParent() && bootVersion.isPresent()) {
            Integer bootMajor = VersionUtils.major(bootVersion.get().value());
            if (bootMajor != null) {
                String def = bootMajor >= 3 ? "17" : "8";
                return Optional.of(new DetectedValue(def, null,
                        "not declared; inferred from the spring-boot-starter-parent " + bootMajor + ".x default",
                        pom.path(), pom.parent().line(), true));
            }
        }
        return Optional.empty();
    }

    private static String resolve(PomModel pom, String value) {
        String r = com.modernizeai.parser.PomParser.resolve(value, pom.properties());
        return r != null ? r : value;
    }
}
