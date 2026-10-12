package com.modernizeai.model;

/**
 * How strongly the evidence supports the finding.
 *
 * <p>VERIFIED means the evidence was deterministically observed (for example, the POM declares
 * Spring Boot 2.7.18) and the rule's condition is documented as incompatible with the target. It
 * still does not prove that the build will fail; a human must review it.
 *
 * <p>POTENTIAL means a pattern matched that frequently needs attention, but correctness depends
 * on context the scanner cannot see (transitive dependencies, runtime configuration, etc.).
 */
public enum Certainty {
    VERIFIED,
    POTENTIAL
}
