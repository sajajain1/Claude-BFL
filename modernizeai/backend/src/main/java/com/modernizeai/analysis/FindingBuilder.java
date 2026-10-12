package com.modernizeai.analysis;

import com.modernizeai.model.Category;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import java.util.ArrayList;
import java.util.List;

/** Fluent builder that keeps rule implementations short and consistent. */
public final class FindingBuilder {
    private final String ruleId;
    private String title;
    private Category category;
    private Severity severity = Severity.MEDIUM;
    private Certainty certainty = Certainty.POTENTIAL;
    private String filePath;
    private Integer line;
    private String evidence;
    private String snippet;
    private String explanation;
    private String nextStep;
    private final List<String> references = new ArrayList<>();

    private FindingBuilder(String ruleId) {
        this.ruleId = ruleId;
    }

    public static FindingBuilder rule(String ruleId) {
        return new FindingBuilder(ruleId);
    }

    public FindingBuilder title(String v) { this.title = v; return this; }
    public FindingBuilder category(Category v) { this.category = v; return this; }
    public FindingBuilder severity(Severity v) { this.severity = v; return this; }
    public FindingBuilder certainty(Certainty v) { this.certainty = v; return this; }
    public FindingBuilder evidence(String v) { this.evidence = SecretRedactor.redact(v); return this; }
    public FindingBuilder explanation(String v) { this.explanation = v; return this; }
    public FindingBuilder nextStep(String v) { this.nextStep = v; return this; }
    public FindingBuilder reference(String v) { this.references.add(v); return this; }

    /** Sets location and extracts a redacted snippet from the file content. */
    public FindingBuilder at(String path, Integer line, String content) {
        this.filePath = path;
        this.line = line;
        this.snippet = Snippets.around(content, line);
        return this;
    }

    public Finding build() {
        if (title == null || category == null || explanation == null || nextStep == null || evidence == null) {
            throw new IllegalStateException("Incomplete finding for rule " + ruleId);
        }
        return new Finding(null, ruleId, title, category, severity, certainty, filePath, line, evidence, snippet,
                explanation, nextStep, List.copyOf(references));
    }
}
