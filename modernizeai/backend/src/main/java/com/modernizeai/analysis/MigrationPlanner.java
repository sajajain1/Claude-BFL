package com.modernizeai.analysis;

import com.modernizeai.model.Category;
import com.modernizeai.model.Finding;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Turns findings into an ordered, deterministic migration plan. No AI involved. */
@Component
public class MigrationPlanner {

    public List<String> plan(ProjectContext ctx, List<Finding> findings) {
        Set<String> rules = findings.stream().map(Finding::ruleId).collect(Collectors.toSet());
        Set<Category> cats = findings.stream().map(Finding::category).collect(Collectors.toSet());
        int target = ctx.targetJavaVersion();
        List<String> steps = new ArrayList<>();

        steps.add("Create a migration branch and make sure the current build and test suite pass before changing anything.");
        if (rules.contains("MIG-POM-001")) {
            steps.add("Fix the malformed pom.xml file(s) and re-run this analysis; dependency checks were skipped for them.");
        }
        if (rules.contains("MIG-DEP-001") || rules.contains("MIG-DEP-002") || rules.contains("MIG-DEP-005")) {
            steps.add("Resolve duplicate, unversioned or unresolved dependencies so the POM is deterministic.");
        }
        Integer bootMajor = ctx.bootMajor();
        if (bootMajor != null && bootMajor < 3) {
            Integer minor = ctx.springBootVersion().map(v -> VersionUtils.minor(v.value())).orElse(null);
            if (bootMajor < 2 || (minor != null && minor < 7)) {
                steps.add("Upgrade Spring Boot to the latest 2.7.x and fix all deprecation warnings (still on javax.*).");
            } else {
                steps.add("Fix Spring Boot 2.7 deprecation warnings while still on 2.7.x.");
            }
        }
        if (cats.contains(Category.JAVA_VERSION) || cats.contains(Category.JDK_API)) {
            steps.add("Move the build and runtime to Java " + target + " (it works with Boot 2.7 too), replacing removed or internal JDK APIs.");
        }
        if (rules.contains("MIG-SPRING-001") || rules.contains("MIG-SPRING-002") || rules.contains("MIG-SPRING-003")
                || rules.contains("MIG-SPRING-004")) {
            steps.add("Rewrite Spring Security configuration as SecurityFilterChain beans with requestMatchers/"
                    + "authorizeHttpRequests (possible on Boot 2.7 / Security 5.8), and re-test access rules.");
        }
        if (cats.contains(Category.DEPENDENCY) && findings.stream().anyMatch(f -> f.ruleId().equals("MIG-DEP-004"))) {
            steps.add("Replace dependencies that have no Boot 3 support (e.g. Springfox, Sleuth, javax.* APIs) with their successors.");
        }
        if (bootMajor == null || bootMajor < 3) {
            steps.add("Upgrade to Spring Boot 3.x and migrate javax.* imports to jakarta.* in one change (OpenRewrite's "
                    + "UpgradeSpringBoot_3_x recipes can automate most of it).");
        } else if (cats.contains(Category.JAKARTA_NAMESPACE)) {
            steps.add("Migrate the remaining javax.* imports to jakarta.*.");
        }
        if (rules.contains("MIG-SPRING-005") || rules.contains("MIG-SPRING-006") || rules.contains("MIG-SPRING-007")
                || rules.contains("MIG-SPRING-008") || rules.contains("MIG-SPRING-009")) {
            steps.add("Replace removed Spring Framework / Boot APIs flagged in this report.");
        }
        if (cats.contains(Category.CONFIGURATION)) {
            steps.add("Rename changed configuration properties and move auto-configuration registrations to "
                    + "AutoConfiguration.imports; use spring-boot-properties-migrator temporarily to catch the rest.");
        }
        steps.add("Run the full test suite plus integration and security tests; review trailing-slash URL matching, which "
                + "Spring Framework 6 disables by default.");
        steps.add("Re-run ModernizeAI on the migrated code to confirm the findings are resolved.");
        return steps;
    }
}
