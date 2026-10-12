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
import com.modernizeai.parser.PomModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** MIG-JAVA-001 (version below target), MIG-JAVA-003 (not declared), MIG-JAVA-004 (source/target mismatch). */
@Component
@Order(2)
public class JavaVersionRule implements MigrationRule {

    @Override
    public String name() {
        return "java-version";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        int target = ctx.targetJavaVersion();
        if (ctx.javaVersion().isPresent()) {
            DetectedValue jv = ctx.javaVersion().get();
            Integer major = VersionUtils.javaMajor(jv.value());
            if (major != null && major < target) {
                out.add(FindingBuilder.rule("MIG-JAVA-001")
                        .title("Java " + major + " is below the target Java " + target)
                        .category(Category.JAVA_VERSION)
                        .severity(Severity.HIGH)
                        .certainty(jv.inferred() ? Certainty.POTENTIAL : Certainty.VERIFIED)
                        .at(jv.path(), jv.line(), ctx.content(jv.path()))
                        .evidence("Java " + (jv.raw() != null ? jv.raw() : major) + " via " + jv.source() + ".")
                        .explanation("Spring Boot 3 requires Java 17 or later. Moving from Java " + major + " also "
                                + "removes the Java EE modules from the JDK (JEP 320, Java 11) and strongly "
                                + "encapsulates JDK internals (JEP 403, Java 17), which can break reflection-heavy "
                                + "libraries and older build plugins.")
                        .nextStep("Set <java.version>" + target + "</java.version> (or maven.compiler.release) and build "
                                + "with JDK " + target + ". Upgrade old plugins (maven-compiler-plugin 3.8+, surefire 2.22+) "
                                + "and libraries such as Lombok, Mockito and ByteBuddy to Java " + target + "-compatible versions.")
                        .reference(References.JAVA17_MIGRATION)
                        .reference(References.JEP_403)
                        .build());
            }
        } else if (ctx.rootPom().isPresent()) {
            PomModel root = ctx.rootPom().get();
            out.add(FindingBuilder.rule("MIG-JAVA-003")
                    .title("Java version is not declared in the POM")
                    .category(Category.JAVA_VERSION)
                    .severity(Severity.MEDIUM)
                    .certainty(Certainty.POTENTIAL)
                    .at(root.path(), null, null)
                    .evidence("No maven.compiler.release/source/target, java.version or maven-compiler-plugin configuration "
                            + "was found in " + root.path() + ".")
                    .explanation("Without an explicit level, the compiler plugin's default (or an unseen parent POM) decides "
                            + "the bytecode level, which makes upgrades unpredictable.")
                    .nextStep("Declare <maven.compiler.release>" + target + "</maven.compiler.release> explicitly.")
                    .reference(References.JAVA17_MIGRATION)
                    .build());
        }

        for (PomModel pom : ctx.poms()) {
            String source = null, targetV = null;
            Integer line = null;
            for (PomModel.Plugin p : pom.plugins()) {
                if (!"maven-compiler-plugin".equals(p.artifactId())) continue;
                PomModel.Property s = p.configuration().get("source");
                PomModel.Property t = p.configuration().get("target");
                if (s != null && t != null) {
                    source = s.value();
                    targetV = t.value();
                    line = s.line();
                }
            }
            if (source == null) {
                source = pom.property("maven.compiler.source");
                targetV = pom.property("maven.compiler.target");
                PomModel.Property sp = pom.properties().get("maven.compiler.source");
                line = sp == null ? null : sp.line();
            }
            Integer s = VersionUtils.javaMajor(source), t = VersionUtils.javaMajor(targetV);
            if (s != null && t != null && !s.equals(t)) {
                out.add(FindingBuilder.rule("MIG-JAVA-004")
                        .title("Compiler source (" + source + ") and target (" + targetV + ") differ")
                        .category(Category.JAVA_VERSION)
                        .severity(Severity.LOW)
                        .certainty(Certainty.VERIFIED)
                        .at(pom.path(), line, ctx.content(pom.path()))
                        .evidence("source=" + source + ", target=" + targetV + " in " + pom.path() + ".")
                        .explanation("Mismatched levels can compile against newer APIs than the target runtime provides.")
                        .nextStep("Replace source/target with a single <maven.compiler.release> value.")
                        .reference(References.JAVA17_MIGRATION)
                        .build());
            }
        }
        return out;
    }
}
