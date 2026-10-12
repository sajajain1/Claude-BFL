package com.modernizeai.service;

import com.modernizeai.model.AiExplanation;
import com.modernizeai.model.AnalysisReport;
import com.modernizeai.model.Category;
import com.modernizeai.model.DependencyInfo;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Renders a report (plus any AI explanations already generated) as GitHub-flavoured Markdown. */
@Component
public class MarkdownReportRenderer {

    public String render(AnalysisReport r, Map<String, AiExplanation> explanations) {
        StringBuilder md = new StringBuilder();
        md.append("# ModernizeAI migration report\n\n");
        md.append("> ").append(r.disclaimer()).append("\n\n");
        md.append("| | |\n|---|---|\n");
        row(md, "Analysis id", r.analysisId());
        row(md, "Generated", r.createdAt().toString());
        row(md, "Upload", r.originalFilename());
        row(md, "Project", nz(r.project().groupId()) + ":" + nz(r.project().artifactId()) + ":" + nz(r.project().version()));
        row(md, "Java version", nz(r.project().javaVersion()) + " (" + nz(r.project().javaVersionSource()) + ")");
        row(md, "Spring Boot version", nz(r.project().springBootVersion()) + " (" + nz(r.project().springBootVersionSource()) + ")");
        row(md, "Target", "Java " + r.targetJavaVersion() + ", Spring Boot " + r.targetSpringBoot());
        row(md, "Java files", String.valueOf(r.project().javaFileCount()));
        md.append('\n');

        md.append("## Findings by severity\n\n| Severity | Count |\n|---|---|\n");
        for (Severity s : Severity.values()) {
            md.append("| ").append(s).append(" | ").append(r.countsBySeverity().getOrDefault(s, 0L)).append(" |\n");
        }
        md.append("\n## Recommended migration steps\n\n");
        for (int i = 0; i < r.recommendedSteps().size(); i++) {
            md.append(i + 1).append(". ").append(r.recommendedSteps().get(i)).append('\n');
        }

        Map<Category, List<Finding>> byCat = r.findings().stream()
                .collect(Collectors.groupingBy(Finding::category, java.util.LinkedHashMap::new, Collectors.toList()));
        md.append("\n## Findings\n");
        if (r.findings().isEmpty()) md.append("\nNo findings. This does not guarantee a migration will succeed.\n");
        for (Map.Entry<Category, List<Finding>> e : byCat.entrySet()) {
            md.append("\n### ").append(e.getKey().label()).append(" (").append(e.getValue().size()).append(")\n");
            for (Finding f : e.getValue()) {
                md.append("\n#### ").append(f.id()).append(" · ").append(f.title()).append("\n\n");
                md.append("- **Rule:** `").append(f.ruleId()).append("` · **Severity:** ").append(f.severity())
                        .append(" · **Certainty:** ").append(f.certainty()).append('\n');
                if (f.filePath() != null) {
                    md.append("- **Location:** `").append(f.filePath()).append(f.line() != null ? ":" + f.line() : "").append("`\n");
                }
                md.append("- **Evidence:** ").append(f.evidence()).append('\n');
                md.append("- **Why it matters:** ").append(f.explanation()).append('\n');
                md.append("- **Suggested next step:** ").append(f.suggestedNextStep()).append('\n');
                if (!f.references().isEmpty()) {
                    md.append("- **References:** ").append(String.join(", ", f.references())).append('\n');
                }
                if (f.snippet() != null) {
                    md.append("\n```\n").append(f.snippet().replace("```", "ʼʼʼ")).append("```\n");
                }
                AiExplanation ai = explanations.get(f.id());
                if (ai != null) {
                    md.append("\n**AI explanation (Claude, confidence: ").append(ai.confidence()).append(") — review before acting:**\n\n");
                    md.append("- *Why it may occur:* ").append(ai.whyItMayOccur()).append('\n');
                    md.append("- *Minimal fix:* ").append(ai.minimalFix()).append('\n');
                    md.append("- *Compatibility:* ").append(ai.compatibilityNotes()).append('\n');
                    if (!ai.missingContext().isEmpty()) {
                        md.append("- *Missing context:* ").append(String.join("; ", ai.missingContext())).append('\n');
                    }
                }
            }
        }
        if (!r.project().keyDependencies().isEmpty()) {
            md.append("\n## Declared dependencies (root POM)\n\n| Dependency | Version | Scope |\n|---|---|---|\n");
            for (DependencyInfo d : r.project().keyDependencies()) {
                md.append("| `").append(d.coordinates()).append("` | ").append(nz(d.declaredVersion())).append(" | ")
                        .append(nz(d.scope())).append(" |\n");
            }
        }
        if (!r.project().parseWarnings().isEmpty()) {
            md.append("\n## Analysis warnings\n\n");
            r.project().parseWarnings().forEach(w -> md.append("- ").append(w).append('\n'));
        }
        return md.toString();
    }

    private static void row(StringBuilder md, String k, String v) {
        md.append("| ").append(k).append(" | ").append(v.replace("|", "\\|")).append(" |\n");
    }

    private static String nz(String s) {
        return s == null ? "unknown" : s;
    }
}
