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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MIG-JAKARTA-001/002: imports from Java EE packages that moved to the jakarta.* namespace in
 * Jakarta EE 9+, which Spring Boot 3 / Spring Framework 6 require.
 *
 * <p>JDK-owned javax packages (javax.sql, javax.crypto, javax.net, javax.naming, javax.swing,
 * javax.xml.parsers, javax.annotation.processing, ...) did NOT move and are deliberately ignored.
 */
@Component
@Order(10)
public class JakartaNamespaceRule implements MigrationRule {

    /** Packages that definitely moved (Jakarta EE 9 namespace change). */
    static final Map<String, String> MOVED = new LinkedHashMap<>();
    /** Packages that moved only for some artifacts; needs human judgement. */
    static final Map<String, String> AMBIGUOUS = new LinkedHashMap<>();

    static {
        MOVED.put("javax.servlet", "jakarta.servlet");
        MOVED.put("javax.persistence", "jakarta.persistence");
        MOVED.put("javax.validation", "jakarta.validation");
        MOVED.put("javax.transaction", "jakarta.transaction");
        MOVED.put("javax.ws.rs", "jakarta.ws.rs");
        MOVED.put("javax.xml.bind", "jakarta.xml.bind");
        MOVED.put("javax.ejb", "jakarta.ejb");
        MOVED.put("javax.faces", "jakarta.faces");
        MOVED.put("javax.jms", "jakarta.jms");
        MOVED.put("javax.mail", "jakarta.mail");
        MOVED.put("javax.websocket", "jakarta.websocket");
        MOVED.put("javax.json", "jakarta.json");
        MOVED.put("javax.el", "jakarta.el");
        MOVED.put("javax.activation", "jakarta.activation");
        MOVED.put("javax.interceptor", "jakarta.interceptor");
        MOVED.put("javax.enterprise", "jakarta.enterprise");
        MOVED.put("javax.batch", "jakarta.batch");
        MOVED.put("javax.security.enterprise", "jakarta.security.enterprise");
        MOVED.put("javax.xml.ws", "jakarta.xml.ws");
        MOVED.put("javax.jws", "jakarta.jws");
        MOVED.put("javax.xml.soap", "jakarta.xml.soap");

        // javax.annotation.PostConstruct etc. moved; but javax.annotation.Nullable/Nonnull come from
        // JSR-305 (findbugs) and javax.annotation.processing is part of the JDK.
        AMBIGUOUS.put("javax.annotation", "jakarta.annotation");
        AMBIGUOUS.put("javax.inject", "jakarta.inject");
    }

    private static final List<String> JSR305 = List.of("javax.annotation.Nullable", "javax.annotation.Nonnull",
            "javax.annotation.CheckForNull", "javax.annotation.ParametersAreNonnullByDefault", "javax.annotation.concurrent",
            "javax.annotation.meta", "javax.annotation.CheckReturnValue", "javax.annotation.processing");

    @Override
    public String name() {
        return "javax-to-jakarta";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        for (JavaSource src : ctx.javaSources()) {
            List<JavaSource.ImportRef> moved = new ArrayList<>();
            List<JavaSource.ImportRef> ambiguous = new ArrayList<>();
            for (JavaSource.ImportRef imp : src.imports()) {
                String n = imp.name();
                if (!n.startsWith("javax.")) continue;
                if (JSR305.stream().anyMatch(n::startsWith)) continue;
                if (matchPrefix(MOVED, n) != null) moved.add(imp);
                else if (matchPrefix(AMBIGUOUS, n) != null) ambiguous.add(imp);
            }
            if (!moved.isEmpty()) {
                out.add(FindingBuilder.rule("MIG-JAKARTA-001")
                        .title("javax.* imports that moved to jakarta.* (" + moved.size() + " in this file)")
                        .category(Category.JAKARTA_NAMESPACE)
                        .severity(Severity.HIGH)
                        .certainty(Certainty.VERIFIED)
                        .at(src.path(), moved.get(0).line(), src.content())
                        .evidence(describe(moved))
                        .explanation("Spring Boot 3 is built on Jakarta EE 9+, where these APIs live under jakarta.*. "
                                + "The imports above reference the old javax.* namespace, so this file will not compile against "
                                + "Boot 3's managed dependencies unless it is migrated. Whether anything else breaks depends on "
                                + "the libraries you use, which this scan cannot see.")
                        .nextStep("Replace the prefixes: " + suggestions(moved, MOVED)
                                + ". Tools such as OpenRewrite's Jakarta recipes or IntelliJ's 'Migrate Packages and Classes' "
                                + "can do this mechanically; review the diff and also update the matching POM dependencies.")
                        .reference(References.BOOT3_MIGRATION)
                        .reference(References.OPENREWRITE_BOOT3)
                        .build());
            }
            if (!ambiguous.isEmpty()) {
                out.add(FindingBuilder.rule("MIG-JAKARTA-002")
                        .title("javax.annotation / javax.inject imports may need jakarta.*")
                        .category(Category.JAKARTA_NAMESPACE)
                        .severity(Severity.MEDIUM)
                        .certainty(Certainty.POTENTIAL)
                        .at(src.path(), ambiguous.get(0).line(), src.content())
                        .evidence(describe(ambiguous))
                        .explanation("Common annotations such as @PostConstruct, @PreDestroy and @Resource, and JSR-330 "
                                + "@Inject, moved to jakarta.annotation / jakarta.inject. Other classes in these packages "
                                + "come from unrelated libraries and did not move, so this needs a manual check.")
                        .nextStep("Check which artifact provides each import. If it is the Java EE common-annotations or "
                                + "JSR-330 API, switch to jakarta.annotation-api / jakarta.inject-api and update the imports.")
                        .reference(References.BOOT3_MIGRATION)
                        .build());
            }
        }
        return out;
    }

    static String matchPrefix(Map<String, String> table, String name) {
        for (String prefix : table.keySet()) {
            if (name.equals(prefix) || name.startsWith(prefix + ".")) return prefix;
        }
        return null;
    }

    private static String describe(List<JavaSource.ImportRef> imports) {
        String list = imports.stream().limit(6)
                .map(i -> "line " + i.line() + ": import " + i.name() + (i.wildcard() ? ".*" : ""))
                .collect(Collectors.joining("; "));
        return imports.size() > 6 ? list + "; … and " + (imports.size() - 6) + " more" : list;
    }

    private static String suggestions(List<JavaSource.ImportRef> imports, Map<String, String> table) {
        return imports.stream().map(i -> matchPrefix(table, i.name())).distinct()
                .map(p -> p + " → " + table.get(p)).collect(Collectors.joining(", "));
    }
}
