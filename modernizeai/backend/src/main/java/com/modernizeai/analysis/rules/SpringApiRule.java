package com.modernizeai.analysis.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MIG-SPRING-0xx: explicitly listed Spring APIs that were removed, moved or deprecated in Spring
 * Framework 6 / Spring Security 6 / Spring Boot 3. Each check is defined in {@link #CHECKS}.
 */
@Component
@Order(30)
public class SpringApiRule implements MigrationRule {

    enum Kind { SUPERTYPE, ANNOTATION, METHOD_CALL, IMPORT }

    /**
     * @param fqn            fully-qualified name for SUPERTYPE/ANNOTATION/IMPORT (prefix for IMPORT)
     * @param simpleName     simple name for SUPERTYPE/ANNOTATION/METHOD_CALL
     * @param requiredImport METHOD_CALL only: the file must import something under this prefix
     */
    record Check(String ruleId, Kind kind, String fqn, String simpleName, String requiredImport, Severity severity,
                 String title, String explanation, String nextStep, String reference) {}

    static final List<Check> CHECKS = List.of(
            new Check("MIG-SPRING-001", Kind.SUPERTYPE,
                    "org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter",
                    "WebSecurityConfigurerAdapter", null, Severity.HIGH,
                    "WebSecurityConfigurerAdapter was removed in Spring Security 6",
                    "Spring Security 5.7 deprecated WebSecurityConfigurerAdapter and Spring Security 6 (used by Boot 3) removed it, "
                            + "so this class will not compile after the upgrade.",
                    "Replace the subclass with @Bean methods returning SecurityFilterChain (and WebSecurityCustomizer / "
                            + "AuthenticationManager beans where needed). Port each configure(...) override deliberately: "
                            + "security behaviour must be re-tested, not just recompiled.",
                    References.SECURITY_WITHOUT_ADAPTER),
            new Check("MIG-SPRING-002", Kind.ANNOTATION,
                    "org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity",
                    "EnableGlobalMethodSecurity", null, Severity.MEDIUM,
                    "@EnableGlobalMethodSecurity is deprecated in favour of @EnableMethodSecurity",
                    "Spring Security 6 deprecates @EnableGlobalMethodSecurity. @EnableMethodSecurity enables @PreAuthorize/"
                            + "@PostAuthorize by default and changes some defaults, so behaviour can differ subtly.",
                    "Switch to @EnableMethodSecurity (set securedEnabled/jsr250Enabled if you rely on them) and re-run "
                            + "authorization tests.",
                    References.SECURITY6_MIGRATION),
            new Check("MIG-SPRING-003", Kind.METHOD_CALL, null, "antMatchers", "org.springframework.security", Severity.HIGH,
                    "antMatchers(...) was removed in Spring Security 6",
                    "antMatchers, mvcMatchers and regexMatchers were removed from the HttpSecurity DSL in Spring Security 6. "
                            + "The scanner matched the method name in a file that imports Spring Security; it cannot resolve the "
                            + "receiver type, so confirm it is the HttpSecurity DSL.",
                    "Use requestMatchers(...). Note that requestMatchers picks an MVC matcher when Spring MVC is present, "
                            + "which matches paths differently from Ant patterns; review each rule.",
                    References.SECURITY6_MIGRATION),
            new Check("MIG-SPRING-003", Kind.METHOD_CALL, null, "mvcMatchers", "org.springframework.security", Severity.HIGH,
                    "mvcMatchers(...) was removed in Spring Security 6",
                    "mvcMatchers was removed from the HttpSecurity DSL in Spring Security 6. The receiver type was not resolved; "
                            + "confirm it is the HttpSecurity DSL.",
                    "Use requestMatchers(...) and review each path rule.",
                    References.SECURITY6_MIGRATION),
            new Check("MIG-SPRING-003", Kind.METHOD_CALL, null, "regexMatchers", "org.springframework.security", Severity.HIGH,
                    "regexMatchers(...) was removed in Spring Security 6",
                    "regexMatchers was removed from the HttpSecurity DSL in Spring Security 6. The receiver type was not resolved.",
                    "Use requestMatchers(RegexRequestMatcher.regexMatcher(...)).",
                    References.SECURITY6_MIGRATION),
            new Check("MIG-SPRING-004", Kind.METHOD_CALL, null, "authorizeRequests", "org.springframework.security", Severity.MEDIUM,
                    "authorizeRequests() is deprecated in favour of authorizeHttpRequests()",
                    "authorizeRequests() uses the legacy FilterSecurityInterceptor and is deprecated in Spring Security 6. "
                            + "authorizeHttpRequests() applies authorization to all dispatcher types by default, which can change "
                            + "behaviour for error pages and forwards.",
                    "Migrate to authorizeHttpRequests(...) and test error, forward and async dispatches.",
                    References.SECURITY6_MIGRATION),
            new Check("MIG-SPRING-005", Kind.SUPERTYPE,
                    "org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter",
                    "WebMvcConfigurerAdapter", null, Severity.HIGH,
                    "WebMvcConfigurerAdapter was removed in Spring Framework 6",
                    "WebMvcConfigurerAdapter was deprecated in Spring 5.0 (WebMvcConfigurer gained default methods) and removed in 6.0.",
                    "Implement WebMvcConfigurer directly instead of extending the adapter; the method signatures are the same.",
                    References.SPRING6_UPGRADE),
            new Check("MIG-SPRING-006", Kind.SUPERTYPE,
                    "org.springframework.web.servlet.handler.HandlerInterceptorAdapter",
                    "HandlerInterceptorAdapter", null, Severity.HIGH,
                    "HandlerInterceptorAdapter was removed in Spring Framework 6",
                    "HandlerInterceptorAdapter was deprecated in 5.3 and removed in 6.0.",
                    "Implement HandlerInterceptor (or AsyncHandlerInterceptor) directly.",
                    References.SPRING6_UPGRADE),
            new Check("MIG-SPRING-007", Kind.IMPORT,
                    "org.springframework.boot.web.server.LocalServerPort", "LocalServerPort", null, Severity.MEDIUM,
                    "@LocalServerPort moved to the spring-boot-test module",
                    "In Spring Boot 3, org.springframework.boot.web.server.LocalServerPort was removed; the annotation lives in "
                            + "org.springframework.boot.test.web.server.",
                    "Change the import to org.springframework.boot.test.web.server.LocalServerPort.",
                    References.BOOT3_MIGRATION),
            new Check("MIG-SPRING-008", Kind.IMPORT,
                    "org.springframework.boot.context.properties.ConstructorBinding", "ConstructorBinding", null, Severity.MEDIUM,
                    "@ConstructorBinding moved and is no longer needed on types",
                    "Spring Boot 3 moved @ConstructorBinding to org.springframework.boot.context.properties.bind and it is "
                            + "only needed on a constructor when there are several.",
                    "Remove type-level @ConstructorBinding; where still required, import it from the .bind package.",
                    References.BOOT3_MIGRATION),
            new Check("MIG-SPRING-009", Kind.IMPORT,
                    "org.springframework.cloud.sleuth", "Sleuth", null, Severity.HIGH,
                    "Spring Cloud Sleuth is not supported on Spring Boot 3",
                    "Sleuth's tracing was moved into Micrometer Tracing; Sleuth has no Boot 3 compatible release.",
                    "Replace Sleuth APIs with Micrometer Tracing (io.micrometer.tracing) and a bridge such as "
                            + "micrometer-tracing-bridge-brave or -otel.",
                    References.MICROMETER_TRACING)
    );

    @Override
    public String name() {
        return "spring-api";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        for (JavaSource src : ctx.javaSources()) {
            for (Check check : CHECKS) {
                switch (check.kind()) {
                    case IMPORT -> checkImport(src, check, out);
                    case SUPERTYPE -> checkSupertype(src, check, out);
                    case ANNOTATION -> checkAnnotation(src, check, out);
                    case METHOD_CALL -> checkMethodCall(src, check, out);
                }
            }
        }
        return out;
    }

    private void checkImport(JavaSource src, Check c, List<Finding> out) {
        for (JavaSource.ImportRef imp : src.imports()) {
            if (imp.name().equals(c.fqn()) || imp.name().startsWith(c.fqn() + ".")) {
                out.add(base(c, src, imp.line(), Certainty.VERIFIED, "line " + imp.line() + ": import " + imp.name()));
                return;
            }
        }
    }

    private void checkSupertype(JavaSource src, Check c, List<Finding> out) {
        if (src.unit().isPresent()) {
            CompilationUnit cu = src.unit().get();
            for (ClassOrInterfaceDeclaration decl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                List<ClassOrInterfaceType> supers = new ArrayList<>(decl.getExtendedTypes());
                supers.addAll(decl.getImplementedTypes());
                for (ClassOrInterfaceType t : supers) {
                    if (!t.getNameAsString().equals(c.simpleName())) continue;
                    Certainty cert = certaintyFor(src, c.fqn(), t.getNameWithScope());
                    if (cert == null) continue;
                    int line = t.getBegin().map(p -> p.line).orElse(decl.getBegin().map(p -> p.line).orElse(1));
                    out.add(base(c, src, line, cert, "line " + line + ": class " + decl.getNameAsString()
                            + " extends/implements " + t.getNameWithScope()));
                }
            }
        } else {
            regexFallback(src, c, Pattern.compile("\\b(extends|implements)\\b[^{]*\\b" + Pattern.quote(c.simpleName()) + "\\b"), out);
        }
    }

    private void checkAnnotation(JavaSource src, Check c, List<Finding> out) {
        if (src.unit().isPresent()) {
            for (AnnotationExpr a : src.unit().get().findAll(AnnotationExpr.class)) {
                String name = a.getNameAsString();
                if (!name.equals(c.simpleName()) && !name.equals(c.fqn())) continue;
                Certainty cert = certaintyFor(src, c.fqn(), name);
                if (cert == null) continue;
                int line = a.getBegin().map(p -> p.line).orElse(1);
                out.add(base(c, src, line, cert, "line " + line + ": @" + name));
            }
        } else {
            regexFallback(src, c, Pattern.compile("@" + Pattern.quote(c.simpleName()) + "\\b"), out);
        }
    }

    private void checkMethodCall(JavaSource src, Check c, List<Finding> out) {
        boolean importsFramework = src.imports().stream().anyMatch(i -> i.name().startsWith(c.requiredImport()));
        if (!importsFramework) return;
        if (src.unit().isPresent()) {
            List<MethodCallExpr> calls = src.unit().get().findAll(MethodCallExpr.class,
                    m -> m.getNameAsString().equals(c.simpleName()));
            if (calls.isEmpty()) return;
            int line = calls.get(0).getBegin().map(p -> p.line).orElse(1);
            String lines = calls.stream().limit(8).map(m -> String.valueOf(m.getBegin().map(p -> p.line).orElse(0)))
                    .distinct().reduce((a, b) -> a + ", " + b).orElse("");
            out.add(base(c, src, line, Certainty.POTENTIAL,
                    calls.size() + " call(s) to " + c.simpleName() + "(...) on line(s) " + lines));
        } else {
            regexFallback(src, c, Pattern.compile("\\." + Pattern.quote(c.simpleName()) + "\\s*\\("), out);
        }
    }

    /**
     * VERIFIED when the FQN is written out or explicitly imported; POTENTIAL when only a wildcard
     * import of the package could supply the name; null when nothing links the name to Spring.
     */
    private static Certainty certaintyFor(JavaSource src, String fqn, String writtenName) {
        if (writtenName.equals(fqn)) return Certainty.VERIFIED;
        String pkg = fqn.substring(0, fqn.lastIndexOf('.'));
        for (JavaSource.ImportRef imp : src.imports()) {
            if (!imp.wildcard() && imp.name().equals(fqn)) return Certainty.VERIFIED;
            if (imp.wildcard() && imp.name().equals(pkg)) return Certainty.POTENTIAL;
        }
        return null;
    }

    private void regexFallback(JavaSource src, Check c, Pattern p, List<Finding> out) {
        String[] lines = src.content().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = p.matcher(lines[i]);
            if (m.find()) {
                out.add(base(c, src, i + 1, Certainty.POTENTIAL, "line " + (i + 1) + " (text match; file could not be parsed): "
                        + lines[i].trim()));
                return;
            }
        }
    }

    private static Finding base(Check c, JavaSource src, int line, Certainty certainty, String evidence) {
        return FindingBuilder.rule(c.ruleId())
                .title(c.title())
                .category(Category.SPRING_API)
                .severity(c.severity())
                .certainty(certainty)
                .at(src.path(), line, src.content())
                .evidence(evidence)
                .explanation(c.explanation())
                .nextStep(c.nextStep())
                .reference(c.reference())
                .build();
    }
}
