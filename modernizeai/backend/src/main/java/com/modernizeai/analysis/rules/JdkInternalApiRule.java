package com.modernizeai.analysis.rules;

import com.modernizeai.analysis.FindingBuilder;
import com.modernizeai.analysis.JavaSource;
import com.modernizeai.analysis.MigrationRule;
import com.modernizeai.analysis.ProjectContext;
import com.modernizeai.analysis.References;
import com.modernizeai.model.Category;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** MIG-JDK-001: imports of JDK-internal sun.* / com.sun.*.internal APIs. */
@Component
@Order(20)
public class JdkInternalApiRule implements MigrationRule {

    @Override
    public String name() {
        return "jdk-internal-api";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        for (JavaSource src : ctx.javaSources()) {
            for (JavaSource.ImportRef imp : src.imports()) {
                String n = imp.name();
                boolean internal = n.startsWith("sun.") || (n.startsWith("com.sun.") && n.contains(".internal."));
                if (!internal) continue;
                boolean removed = n.startsWith("sun.misc.BASE64");
                out.add(FindingBuilder.rule("MIG-JDK-001")
                        .title("JDK internal API import: " + n)
                        .category(Category.JDK_API)
                        .severity(removed ? Severity.HIGH : Severity.MEDIUM)
                        .certainty(removed ? Certainty.VERIFIED : Certainty.POTENTIAL)
                        .at(src.path(), imp.line(), src.content())
                        .evidence("line " + imp.line() + ": import " + n)
                        .explanation(removed
                                ? "sun.misc.BASE64Encoder/Decoder were removed in Java 9; this code will not compile on Java 17."
                                : "JDK internals are strongly encapsulated since Java 17 (JEP 403). Access may fail at compile "
                                        + "time or at runtime with IllegalAccessError, depending on the class.")
                        .nextStep(removed ? "Use java.util.Base64 instead."
                                : "Replace with a supported public API. As a temporary measure only, --add-exports/--add-opens can restore access.")
                        .reference(References.JEP_403)
                        .build());
            }
        }
        return out;
    }
}
