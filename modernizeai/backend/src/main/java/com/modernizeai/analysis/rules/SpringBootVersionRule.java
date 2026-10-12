package com.modernizeai.analysis.rules;

import com.modernizeai.analysis.DetectedValue;
import com.modernizeai.analysis.FindingBuilder;
import com.modernizeai.analysis.MigrationRule;
import com.modernizeai.analysis.ProjectContext;
import com.modernizeai.analysis.References;
import com.modernizeai.analysis.VersionUtils;
import com.modernizeai.model.Category;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** MIG-BOOT-001..003: the Spring Boot generation the project is on. */
@Component
@Order(1)
public class SpringBootVersionRule implements MigrationRule {

    @Override
    public String name() {
        return "spring-boot-version";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        if (ctx.springBootVersion().isEmpty()) {
            if (ctx.usesSpringBoot()) {
                String pom = ctx.rootPom().map(p -> p.path()).orElse("pom.xml");
                out.add(FindingBuilder.rule("MIG-BOOT-003")
                        .title("Spring Boot is used but its version could not be determined")
                        .category(Category.SPRING_BOOT_UPGRADE)
                        .severity(Severity.INFO)
                        .certainty(Certainty.POTENTIAL)
                        .at(pom, null, null)
                        .evidence("org.springframework.boot dependencies are declared, but no spring-boot-starter-parent, "
                                + "spring-boot-dependencies BOM or explicit version was found in the uploaded POMs.")
                        .explanation("The version is probably inherited from a corporate parent POM that was not uploaded. "
                                + "Version-specific checks were skipped rather than guessed.")
                        .nextStep("Run `mvn help:effective-pom` locally to see the resolved Spring Boot version.")
                        .build());
            }
            return out;
        }
        DetectedValue boot = ctx.springBootVersion().get();
        Integer major = VersionUtils.major(boot.value());
        if (major == null) return out;
        String content = ctx.content(boot.path());

        if (major < 3) {
            Integer minor = VersionUtils.minor(boot.value());
            boolean belowTwoSeven = major < 2 || (minor != null && minor < 7);
            String path = belowTwoSeven
                    ? "Upgrade to the latest 2.7.x first and fix its deprecation warnings, then move to 3.x."
                    : "You are on 2.7.x, the recommended stepping stone. Fix 2.7 deprecation warnings, then move to 3.x.";
            out.add(FindingBuilder.rule("MIG-BOOT-001")
                    .title("Spring Boot " + boot.value() + " requires a major upgrade to reach 3.x")
                    .category(Category.SPRING_BOOT_UPGRADE)
                    .severity(Severity.HIGH)
                    .certainty(Certainty.VERIFIED)
                    .at(boot.path(), boot.line(), content)
                    .evidence("Spring Boot " + boot.value() + " detected via " + boot.source() + ".")
                    .explanation("Spring Boot 3 requires Java 17+, moves to Jakarta EE 9+ (javax.* → jakarta.*), "
                            + "upgrades to Spring Framework 6 and Spring Security 6, and renames or removes several "
                            + "configuration properties. Other findings in this report flag the specific places "
                            + "detected in your code; there may be more that static analysis cannot see.")
                    .nextStep(path + " Temporarily add spring-boot-properties-migrator (runtime scope) to report "
                            + "renamed properties at startup, and remove it after migrating.")
                    .reference(References.BOOT3_MIGRATION)
                    .reference(References.OPENREWRITE_BOOT3)
                    .build());
        } else if (major == 3 && VersionUtils.compare(boot.value(), "3.2.0") < 0) {
            out.add(FindingBuilder.rule("MIG-BOOT-002")
                    .title("Spring Boot " + boot.value() + " is an older 3.x line")
                    .category(Category.SPRING_BOOT_UPGRADE)
                    .severity(Severity.LOW)
                    .certainty(Certainty.VERIFIED)
                    .at(boot.path(), boot.line(), content)
                    .evidence("Spring Boot " + boot.value() + " detected via " + boot.source() + ".")
                    .explanation("The project is already on Boot 3, so the large Jakarta/Java 17 migration is done. "
                            + "Older 3.x lines stop receiving open-source support sooner.")
                    .nextStep("Plan a minor upgrade to a supported 3.x release and review its release notes.")
                    .reference(References.BOOT3_RELEASE_NOTES)
                    .build());
        }
        return out;
    }
}
