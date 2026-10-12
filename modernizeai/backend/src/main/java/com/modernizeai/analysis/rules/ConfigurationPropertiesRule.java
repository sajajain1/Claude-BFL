package com.modernizeai.analysis.rules;

import com.modernizeai.analysis.FindingBuilder;
import com.modernizeai.analysis.MigrationRule;
import com.modernizeai.analysis.ProjectContext;
import com.modernizeai.analysis.References;
import com.modernizeai.model.Category;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MIG-CONFIG-001: application properties renamed or removed in Spring Boot 3.
 * MIG-CONFIG-002: auto-configuration registered via spring.factories (no longer read by Boot 3).
 */
@Component
@Order(40)
public class ConfigurationPropertiesRule implements MigrationRule {

    /** Old key prefix -> replacement (or null when removed). Prefixes ending in "." match subtrees. */
    record Rename(String oldPrefix, String replacement, String note) {}

    static final List<Rename> RENAMES = List.of(
            new Rename("spring.redis.", "spring.data.redis.", "Redis properties moved under spring.data.redis."),
            new Rename("spring.data.cassandra.", "spring.cassandra.", "Cassandra properties moved to spring.cassandra."),
            new Rename("server.max-http-header-size", "server.max-http-request-header-size", "Renamed in Boot 3.0."),
            new Rename("spring.jpa.hibernate.use-new-id-generator-mappings", null,
                    "Removed: Hibernate 6 no longer supports switching back to the old ID generators."),
            new Rename("management.metrics.export.", "management.<product>.metrics.export.",
                    "Metrics export properties moved, e.g. management.metrics.export.prometheus.* → management.prometheus.metrics.export.*."),
            new Rename("management.trace.http.", "management.httpexchanges.recording.",
                    "HTTP tracing was renamed to HTTP exchanges in Boot 3."),
            new Rename("spring.sleuth.", "management.tracing.", "Sleuth is replaced by Micrometer Tracing (management.tracing.*)."),
            new Rename("spring.mvc.throw-exception-if-no-handler-found", null,
                    "Deprecated in Boot 3.2: NoHandlerFoundException is now thrown by default via the problem-details support."));

    private static final Pattern PROPS_LINE = Pattern.compile("^\\s*([A-Za-z0-9_.\\-\\[\\]]+)\\s*[=:]");
    private static final Pattern YAML_KEY = Pattern.compile("^(\\s*)(?:-\\s+)?([A-Za-z0-9_.\\-\"']+)\\s*:(?:\\s+(.*))?$");

    @Override
    public String name() {
        return "configuration-properties";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        for (Map.Entry<String, String> e : ctx.files().all().entrySet()) {
            String path = e.getKey();
            String content = e.getValue();
            if (path.endsWith("spring.factories")) {
                checkSpringFactories(path, content, ctx, out);
                continue;
            }
            if (!path.contains("src/main/resources")) continue;
            List<KeyAt> keys;
            if (path.endsWith(".properties")) keys = propertiesKeys(content);
            else if (path.endsWith(".yml") || path.endsWith(".yaml")) keys = yamlKeys(content);
            else continue;
            for (KeyAt k : keys) {
                for (Rename r : RENAMES) {
                    if (k.key().equals(r.oldPrefix()) || (r.oldPrefix().endsWith(".") && k.key().startsWith(r.oldPrefix()))) {
                        String suggestion = r.replacement() == null ? "remove it"
                                : "use " + (r.oldPrefix().endsWith(".") ? k.key().replaceFirst(Pattern.quote(r.oldPrefix()),
                                Matcher.quoteReplacement(r.replacement())) : r.replacement());
                        out.add(FindingBuilder.rule("MIG-CONFIG-001")
                                .title("Property '" + k.key() + "' was renamed or removed in Spring Boot 3")
                                .category(Category.CONFIGURATION)
                                .severity(Severity.MEDIUM)
                                .certainty(Certainty.VERIFIED)
                                .at(path, k.line(), content)
                                .evidence("line " + k.line() + ": " + k.key())
                                .explanation(r.note() + " Spring Boot silently ignores unknown properties, so the old key "
                                        + "would stop having an effect without any error.")
                                .nextStep("After upgrading, " + suggestion + ". spring-boot-properties-migrator reports these at startup.")
                                .reference(References.BOOT3_MIGRATION)
                                .build());
                        break;
                    }
                }
            }
        }
        return out;
    }

    private void checkSpringFactories(String path, String content, ProjectContext ctx, List<Finding> out) {
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("org.springframework.boot.autoconfigure.EnableAutoConfiguration")) {
                out.add(FindingBuilder.rule("MIG-CONFIG-002")
                        .title("Auto-configuration registered in spring.factories")
                        .category(Category.CONFIGURATION)
                        .severity(Severity.HIGH)
                        .certainty(Certainty.VERIFIED)
                        .at(path, i + 1, content)
                        .evidence("line " + (i + 1) + ": EnableAutoConfiguration key in " + path)
                        .explanation("Spring Boot 3 no longer loads auto-configurations from spring.factories; they would "
                                + "silently stop being applied.")
                        .nextStep("List the classes, one per line, in META-INF/spring/"
                                + "org.springframework.boot.autoconfigure.AutoConfiguration.imports and annotate them with @AutoConfiguration.")
                        .reference(References.BOOT3_MIGRATION)
                        .build());
                return;
            }
        }
    }

    record KeyAt(String key, int line) {}

    static List<KeyAt> propertiesKeys(String content) {
        List<KeyAt> keys = new ArrayList<>();
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.trim().startsWith("#") || l.trim().startsWith("!")) continue;
            Matcher m = PROPS_LINE.matcher(l);
            if (m.find()) keys.add(new KeyAt(m.group(1), i + 1));
        }
        return keys;
    }

    /**
     * Minimal indentation-based YAML key flattener; good enough for typical application.yml files.
     * Only leaf keys (keys with an inline value) are returned, so a renamed subtree is reported once
     * per property rather than once per nesting level.
     */
    static List<KeyAt> yamlKeys(String content) {
        List<KeyAt> keys = new ArrayList<>();
        Deque<int[]> indents = new ArrayDeque<>();
        Deque<String> names = new ArrayDeque<>();
        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.isBlank() || l.trim().startsWith("#")) continue;
            if (l.startsWith("---")) {
                indents.clear();
                names.clear();
                continue;
            }
            Matcher m = YAML_KEY.matcher(l);
            if (!m.find()) continue;
            int indent = m.group(1).length();
            String key = m.group(2).replace("\"", "").replace("'", "");
            while (!indents.isEmpty() && indents.peek()[0] >= indent) {
                indents.pop();
                names.pop();
            }
            indents.push(new int[] {indent});
            names.push(key);
            String value = m.group(3);
            boolean leaf = value != null && !value.isBlank() && !value.trim().startsWith("#");
            if (leaf) {
                List<String> path = new ArrayList<>(names);
                java.util.Collections.reverse(path);
                keys.add(new KeyAt(String.join(".", path), i + 1));
            }
        }
        return keys;
    }
}
