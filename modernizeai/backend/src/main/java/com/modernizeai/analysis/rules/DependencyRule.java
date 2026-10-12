package com.modernizeai.analysis.rules;

import com.modernizeai.analysis.FindingBuilder;
import com.modernizeai.analysis.MigrationRule;
import com.modernizeai.analysis.ProjectContext;
import com.modernizeai.analysis.References;
import com.modernizeai.analysis.VersionUtils;
import com.modernizeai.model.Category;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.DependencyInfo;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import com.modernizeai.parser.PomModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dependency checks that can be decided from the POM text alone (Maven is never executed):
 * <ul>
 *   <li>MIG-DEP-001 the same dependency declared twice with different versions</li>
 *   <li>MIG-DEP-002 a dependency with no version and no visible version management</li>
 *   <li>MIG-DEP-003 an explicit version overriding one managed by Spring Boot</li>
 *   <li>MIG-DEP-004 artifacts known to be incompatible with Spring Boot 3 / Jakarta EE</li>
 *   <li>MIG-DEP-005 a version that references an undefined property</li>
 *   <li>MIG-DEP-006 Spring Boot artifacts pinned to a different version than the Boot parent</li>
 * </ul>
 */
@Component
@Order(50)
public class DependencyRule implements MigrationRule {

    /** groupId prefixes whose versions Spring Boot's dependency management controls. */
    static final List<String> BOOT_MANAGED_GROUPS = List.of(
            "org.springframework", "com.fasterxml.jackson", "org.hibernate", "org.apache.tomcat", "ch.qos.logback",
            "org.slf4j", "org.junit", "org.mockito", "org.assertj", "io.micrometer", "org.flywaydb", "org.liquibase",
            "com.h2database", "org.postgresql", "com.mysql", "org.mariadb.jdbc", "com.zaxxer", "org.apache.logging.log4j",
            "org.yaml", "jakarta.", "org.thymeleaf", "io.projectreactor", "org.aspectj", "org.apache.kafka", "com.google.code.gson",
            "org.projectlombok");

    record Incompatible(String groupId, String artifactId, Severity severity, Certainty certainty, String reason, String replacement,
                        String reference) {}

    /** artifactId "*" matches every artifact in the group. */
    static final List<Incompatible> INCOMPATIBLE = List.of(
            new Incompatible("io.springfox", "*", Severity.HIGH, Certainty.VERIFIED,
                    "Springfox is unmaintained and does not support Spring Boot 3 / Jakarta EE.",
                    "Replace with org.springdoc:springdoc-openapi-starter-webmvc-ui 2.x and migrate Docket configuration "
                            + "and @Api* annotations to OpenAPI 3 (@Operation, @Tag...).", References.SPRINGDOC),
            new Incompatible("javax.servlet", "*", Severity.HIGH, Certainty.VERIFIED,
                    "Java EE Servlet API (javax.servlet) cannot be used with Spring Framework 6.",
                    "Use jakarta.servlet:jakarta.servlet-api (managed by Boot 3) or simply rely on the starter.", References.BOOT3_MIGRATION),
            new Incompatible("javax.persistence", "*", Severity.HIGH, Certainty.VERIFIED,
                    "javax.persistence is replaced by jakarta.persistence in Hibernate 6 / Boot 3.",
                    "Use jakarta.persistence:jakarta.persistence-api (via spring-boot-starter-data-jpa).", References.BOOT3_MIGRATION),
            new Incompatible("javax.validation", "*", Severity.HIGH, Certainty.VERIFIED,
                    "Bean Validation moved to jakarta.validation.",
                    "Use spring-boot-starter-validation (jakarta.validation-api + Hibernate Validator 8).", References.BOOT3_MIGRATION),
            new Incompatible("javax.xml.bind", "*", Severity.MEDIUM, Certainty.VERIFIED,
                    "JAXB moved to jakarta.xml.bind (and was removed from the JDK in Java 11).",
                    "Use jakarta.xml.bind:jakarta.xml.bind-api with org.glassfish.jaxb:jaxb-runtime.", References.JEP_320),
            new Incompatible("javax.annotation", "javax.annotation-api", Severity.MEDIUM, Certainty.VERIFIED,
                    "Common annotations moved to jakarta.annotation.",
                    "Use jakarta.annotation:jakarta.annotation-api.", References.BOOT3_MIGRATION),
            new Incompatible("javax.transaction", "*", Severity.MEDIUM, Certainty.VERIFIED,
                    "JTA moved to jakarta.transaction.",
                    "Use jakarta.transaction:jakarta.transaction-api.", References.BOOT3_MIGRATION),
            new Incompatible("mysql", "mysql-connector-java", Severity.MEDIUM, Certainty.VERIFIED,
                    "The MySQL driver moved to com.mysql:mysql-connector-j; Boot 3 only manages the new coordinates, "
                            + "so the old ones lose their managed version.",
                    "Change to com.mysql:mysql-connector-j.", References.BOOT3_MIGRATION),
            new Incompatible("org.springframework.cloud", "spring-cloud-starter-sleuth", Severity.HIGH, Certainty.VERIFIED,
                    "Spring Cloud Sleuth has no Spring Boot 3 compatible release.",
                    "Use io.micrometer:micrometer-tracing-bridge-brave (or -otel) with spring-boot-starter-actuator.",
                    References.MICROMETER_TRACING),
            new Incompatible("net.sf.ehcache", "ehcache", Severity.MEDIUM, Certainty.VERIFIED,
                    "Spring Boot 3 removed support for Ehcache 2.",
                    "Move to Ehcache 3 (org.ehcache:ehcache, jakarta classifier) via JCache, or another cache provider.",
                    References.BOOT3_MIGRATION),
            new Incompatible("org.apache.httpcomponents", "httpclient", Severity.MEDIUM, Certainty.POTENTIAL,
                    "Spring Framework 6's HttpComponentsClientHttpRequestFactory requires Apache HttpClient 5; HttpClient 4 "
                            + "is no longer supported by RestTemplate.",
                    "If RestTemplate uses Apache HttpClient, switch to org.apache.httpcomponents.client5:httpclient5.",
                    References.SPRING6_UPGRADE),
            new Incompatible("org.hibernate", "hibernate-validator", Severity.LOW, Certainty.VERIFIED,
                    "Hibernate Validator's groupId changed to org.hibernate.validator; 6.x/7.x use javax/older APIs.",
                    "Remove the explicit dependency and use spring-boot-starter-validation.", References.BOOT3_MIGRATION),
            new Incompatible("junit", "junit", Severity.LOW, Certainty.POTENTIAL,
                    "JUnit 4 tests do not run on the JUnit Platform without the vintage engine, which Spring Boot starters "
                            + "stopped including in 2.4.",
                    "Migrate tests to JUnit 5 (OpenRewrite has a recipe), or add junit-vintage-engine as a stop-gap.",
                    References.BOOT3_MIGRATION));

    @Override
    public String name() {
        return "dependencies";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        Set<String> projectArtifacts = ctx.poms().stream().map(p -> p.groupId() + ":" + p.artifactId()).collect(Collectors.toSet());
        for (PomModel pom : ctx.poms()) {
            String content = ctx.content(pom.path());
            duplicates(pom, content, out);
            boolean bootManaged = pom.hasSpringBootParent() || pom.managedDependencies().stream()
                    .anyMatch(d -> "spring-boot-dependencies".equals(d.artifactId()));
            boolean hasParent = pom.parent() != null;
            boolean importsBom = pom.managedDependencies().stream().anyMatch(d -> "import".equals(d.scope()));
            Set<String> locallyManaged = pom.managedDependencies().stream().map(DependencyInfo::coordinates).collect(Collectors.toSet());

            for (DependencyInfo d : pom.dependencies()) {
                if (d.declaredVersion() == null) {
                    missingVersion(pom, content, d, bootManaged, hasParent, importsBom, locallyManaged, projectArtifacts, out);
                } else {
                    if (d.resolvedVersion() == null) {
                        unresolved(pom, content, d, hasParent, out);
                    } else if (bootManaged && isBootManaged(d.groupId()) && !"org.springframework.boot".equals(d.groupId())
                            && !d.declaredVersion().contains("project.version")) {
                        out.add(FindingBuilder.rule("MIG-DEP-003")
                                .title("Explicit version overrides Spring Boot's managed version: " + d.coordinates())
                                .category(Category.DEPENDENCY)
                                .severity(Severity.MEDIUM)
                                .certainty(Certainty.POTENTIAL)
                                .at(pom.path(), d.line(), content)
                                .evidence(d.coordinates() + " pinned to " + d.declaredVersion()
                                        + (d.declaredVersion().equals(d.resolvedVersion()) ? "" : " (= " + d.resolvedVersion() + ")")
                                        + " while Spring Boot manages this group.")
                                .explanation("Pinned versions do not move when Spring Boot is upgraded, which is a common cause "
                                        + "of NoSuchMethodError / ClassNotFoundException after a Boot 3 upgrade. The pin may be "
                                        + "intentional (e.g. a security fix), so this is flagged for review only.")
                                .nextStep("Remove the <version> so Boot manages it, or override via the corresponding Boot "
                                        + "version property, and document why if the pin must stay.")
                                .reference(References.BOOT3_MIGRATION)
                                .build());
                    }
                    if ("org.springframework.boot".equals(d.groupId()) && d.resolvedVersion() != null
                            && ctx.springBootVersion().isPresent()
                            && VersionUtils.compare(d.resolvedVersion(), ctx.springBootVersion().get().value()) != 0) {
                        out.add(FindingBuilder.rule("MIG-DEP-006")
                                .title("Spring Boot artifact pinned to a different version: " + d.artifactId())
                                .category(Category.DEPENDENCY)
                                .severity(Severity.HIGH)
                                .certainty(Certainty.VERIFIED)
                                .at(pom.path(), d.line(), content)
                                .evidence(d.coordinates() + ":" + d.resolvedVersion() + " vs Spring Boot "
                                        + ctx.springBootVersion().get().value() + " (" + ctx.springBootVersion().get().source() + ")")
                                .explanation("Mixing Spring Boot module versions is unsupported and typically fails at startup.")
                                .nextStep("Remove the explicit version so it follows the Boot parent/BOM.")
                                .reference(References.BOOT3_MIGRATION)
                                .build());
                    }
                }
                incompatible(pom, content, d, out);
            }
            for (DependencyInfo d : pom.managedDependencies()) {
                if (d.declaredVersion() != null && d.resolvedVersion() == null) unresolved(pom, content, d, hasParent, out);
                incompatible(pom, content, d, out);
            }
        }
        return out;
    }

    private void duplicates(PomModel pom, String content, List<Finding> out) {
        Map<String, List<DependencyInfo>> byKey = new LinkedHashMap<>();
        for (DependencyInfo d : pom.dependencies()) {
            String key = d.coordinates() + ":" + (d.type() == null ? "jar" : d.type());
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }
        for (List<DependencyInfo> group : byKey.values()) {
            if (group.size() < 2) continue;
            Set<String> versions = group.stream().map(d -> String.valueOf(d.resolvedVersion() != null ? d.resolvedVersion() : d.declaredVersion()))
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            DependencyInfo last = group.get(group.size() - 1);
            String where = group.stream().map(d -> "line " + d.line() + " (" + (d.declaredVersion() == null ? "no version" : d.declaredVersion()) + ")")
                    .collect(Collectors.joining(", "));
            boolean conflict = versions.size() > 1;
            out.add(FindingBuilder.rule("MIG-DEP-001")
                    .title((conflict ? "Conflicting duplicate declarations of " : "Duplicate declaration of ") + group.get(0).coordinates())
                    .category(Category.DEPENDENCY)
                    .severity(conflict ? Severity.HIGH : Severity.LOW)
                    .certainty(Certainty.VERIFIED)
                    .at(pom.path(), last.line(), content)
                    .evidence("Declared " + group.size() + " times in " + pom.path() + ": " + where + ".")
                    .explanation(conflict
                            ? "Maven warns about duplicate declarations and uses the last one, so the effective version may not be "
                                    + "the one developers expect. Future Maven versions may reject this."
                            : "Duplicate declarations are redundant and trigger Maven warnings.")
                    .nextStep("Keep a single declaration with the intended version (prefer dependencyManagement for shared versions).")
                    .build());
        }
    }

    private void missingVersion(PomModel pom, String content, DependencyInfo d, boolean bootManaged, boolean hasParent, boolean importsBom,
                                Set<String> locallyManaged, Set<String> projectArtifacts, List<Finding> out) {
        if (locallyManaged.contains(d.coordinates()) || projectArtifacts.contains(d.coordinates())) return;
        if (bootManaged && isBootManaged(d.groupId())) return;
        if ("import".equals(d.scope())) return;
        boolean verified = !hasParent && !importsBom;
        if (!verified && bootManaged) {
            // Boot manages hundreds of libraries beyond our prefix list; don't flag unknown ones as problems.
            out.add(FindingBuilder.rule("MIG-DEP-002")
                    .title("Version of " + d.coordinates() + " must come from inherited management")
                    .category(Category.DEPENDENCY)
                    .severity(Severity.INFO)
                    .certainty(Certainty.POTENTIAL)
                    .at(pom.path(), d.line(), content)
                    .evidence(d.coordinates() + " has no <version>; it is not in this POM's dependencyManagement.")
                    .explanation("This is fine if Spring Boot or another imported BOM manages it. If not, Maven fails with "
                            + "'dependencies.dependency.version is missing'. After a Boot upgrade, libraries can drop out of "
                            + "Boot's managed set (e.g. mysql-connector-java), which causes exactly this failure.")
                    .nextStep("Check `mvn help:effective-pom` after upgrading; add an explicit version if it is no longer managed.")
                    .build());
            return;
        }
        out.add(FindingBuilder.rule("MIG-DEP-002")
                .title("Missing version for " + d.coordinates())
                .category(Category.DEPENDENCY)
                .severity(verified ? Severity.HIGH : Severity.LOW)
                .certainty(verified ? Certainty.VERIFIED : Certainty.POTENTIAL)
                .at(pom.path(), d.line(), content)
                .evidence(d.coordinates() + " has no <version>, and " + (verified
                        ? "the POM has no parent, imported BOM or dependencyManagement entry for it."
                        : "it must be supplied by the parent " + (pom.parent() != null ? pom.parent().artifactId() : "") + " (not uploaded)."))
                .explanation(verified ? "Maven cannot resolve a dependency without a version; the build fails."
                        : "The version is expected from a parent POM or BOM that was not part of the upload, so it could not be checked.")
                .nextStep(verified ? "Add a <version>, or manage it in <dependencyManagement>."
                        : "Confirm the parent manages this artifact for the target versions.")
                .build());
    }

    private void unresolved(PomModel pom, String content, DependencyInfo d, boolean hasParent, List<Finding> out) {
        out.add(FindingBuilder.rule("MIG-DEP-005")
                .title("Version property not defined in this POM: " + d.declaredVersion())
                .category(Category.DEPENDENCY)
                .severity(hasParent ? Severity.INFO : Severity.HIGH)
                .certainty(hasParent ? Certainty.POTENTIAL : Certainty.VERIFIED)
                .at(pom.path(), d.line(), content)
                .evidence(d.coordinates() + " uses " + d.declaredVersion() + ", which is not declared in <properties> of " + pom.path() + ".")
                .explanation(hasParent ? "The property may be inherited from the parent POM, which was not uploaded."
                        : "With no parent POM, Maven cannot resolve this property and the build fails.")
                .nextStep(hasParent ? "Confirm the parent defines it for the versions you are upgrading to."
                        : "Define the property in <properties> or use a literal version.")
                .build());
    }

    private void incompatible(PomModel pom, String content, DependencyInfo d, List<Finding> out) {
        for (Incompatible inc : INCOMPATIBLE) {
            if (!inc.groupId().equals(d.groupId())) continue;
            if (!inc.artifactId().equals("*") && !inc.artifactId().equals(d.artifactId())) continue;
            out.add(FindingBuilder.rule("MIG-DEP-004")
                    .title("Dependency not compatible with Spring Boot 3: " + d.coordinates())
                    .category(Category.DEPENDENCY)
                    .severity(inc.severity())
                    .certainty(inc.certainty())
                    .at(pom.path(), d.line(), content)
                    .evidence(d.coordinates() + (d.declaredVersion() != null ? ":" + d.declaredVersion() : "")
                            + (d.managed() ? " (dependencyManagement)" : "") + " declared in " + pom.path() + ".")
                    .explanation(inc.reason())
                    .nextStep(inc.replacement())
                    .reference(inc.reference())
                    .build());
            return;
        }
    }

    static boolean isBootManaged(String groupId) {
        return groupId != null && BOOT_MANAGED_GROUPS.stream().anyMatch(groupId::startsWith);
    }
}
