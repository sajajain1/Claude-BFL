package com.modernizeai.analysis;

import com.modernizeai.model.Finding;
import java.util.List;

/**
 * A deterministic migration check. Implementations must be side-effect free, must not execute
 * project code, and must report uncertainty honestly via {@link com.modernizeai.model.Certainty}.
 */
public interface MigrationRule {

    /** Short name used in logs and docs. */
    String name();

    /** Findings with {@code id} left null; the scanner assigns ids. */
    List<Finding> evaluate(ProjectContext context);
}
