package com.modernizeai.analysis.rules;

import com.modernizeai.analysis.FindingBuilder;
import com.modernizeai.analysis.MigrationRule;
import com.modernizeai.analysis.ProjectContext;
import com.modernizeai.model.Category;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** MIG-POM-001: a pom.xml could not be parsed. Reported as a finding so analysis can continue. */
@Component
@Order(0)
public class PomErrorRule implements MigrationRule {

    @Override
    public String name() {
        return "pom-parse-errors";
    }

    @Override
    public List<Finding> evaluate(ProjectContext ctx) {
        List<Finding> out = new ArrayList<>();
        for (Map.Entry<String, ProjectContext.PomParseError> e : ctx.pomErrors().entrySet()) {
            out.add(FindingBuilder.rule("MIG-POM-001")
                    .title("pom.xml could not be parsed")
                    .category(Category.DEPENDENCY)
                    .severity(Severity.HIGH)
                    .certainty(Certainty.VERIFIED)
                    .at(e.getKey(), e.getValue().line(), ctx.content(e.getKey()))
                    .evidence(e.getValue().message())
                    .explanation("The POM is not well-formed, so dependency and version checks for it were skipped. "
                            + "Maven would also refuse to build it.")
                    .nextStep("Fix the XML error (run `mvn validate` locally) and re-run the analysis.")
                    .build());
        }
        return out;
    }
}
