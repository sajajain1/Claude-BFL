package com.modernizeai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.modernizeai.model.AnalysisReport;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import com.modernizeai.service.AnalysisService;
import com.modernizeai.support.TestSupport;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** End-to-end scan of the committed demo projects through the real Spring context. */
@SpringBootTest
class SampleProjectScanTest {

    @Autowired
    AnalysisService service;

    @Test
    void legacyShopProducesExpectedFindings() {
        AnalysisReport r = service.analyze("legacy-shop.zip", TestSupport.zipDirectory(TestSupport.SAMPLES.resolve("legacy-shop")), 17);

        assertThat(r.project().javaVersion()).isEqualTo("8");
        assertThat(r.project().springBootVersion()).isEqualTo("2.5.14");
        assertThat(r.project().artifactId()).isEqualTo("legacy-shop");

        Set<String> rules = r.findings().stream().map(Finding::ruleId).collect(Collectors.toSet());
        assertThat(rules).contains(
                "MIG-BOOT-001", "MIG-JAVA-001", "MIG-JAKARTA-001", "MIG-JAKARTA-002",
                "MIG-SPRING-001", "MIG-SPRING-002", "MIG-SPRING-003", "MIG-SPRING-004", "MIG-SPRING-005",
                "MIG-SPRING-006", "MIG-SPRING-007", "MIG-SPRING-009",
                "MIG-DEP-001", "MIG-DEP-003", "MIG-DEP-004", "MIG-DEP-005",
                "MIG-CONFIG-001", "MIG-CONFIG-002", "MIG-JDK-001");
        assertThat(r.countsBySeverity().get(Severity.HIGH)).isGreaterThan(5);
        assertThat(r.recommendedSteps()).anyMatch(s -> s.contains("2.7.x"));

        // Every finding carries the required fields.
        assertThat(r.findings()).allSatisfy(f -> {
            assertThat(f.id()).matches("F-\\d{3}");
            assertThat(f.evidence()).isNotBlank();
            assertThat(f.explanation()).isNotBlank();
            assertThat(f.suggestedNextStep()).isNotBlank();
            assertThat(f.certainty()).isNotNull();
        });
        // The demo password never leaves the scanner.
        assertThat(r.findings()).noneMatch(f -> (f.snippet() != null && f.snippet().contains("change-me-demo-only"))
                || f.evidence().contains("change-me-demo-only"));
    }

    @Test
    void modernServiceHasNoFalsePositives() {
        AnalysisReport r = service.analyze("modern-service.zip", TestSupport.zipDirectory(TestSupport.SAMPLES.resolve("modern-service")), 17);
        assertThat(r.project().springBootVersion()).isEqualTo("3.2.5");
        assertThat(r.project().javaVersion()).isEqualTo("17");
        assertThat(r.findings()).isEmpty();
    }
}
