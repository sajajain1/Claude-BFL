package com.modernizeai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.modernizeai.analysis.rules.ConfigurationPropertiesRule;
import com.modernizeai.analysis.rules.DependencyRule;
import com.modernizeai.analysis.rules.JakartaNamespaceRule;
import com.modernizeai.analysis.rules.JavaVersionRule;
import com.modernizeai.analysis.rules.JdkInternalApiRule;
import com.modernizeai.analysis.rules.PomErrorRule;
import com.modernizeai.analysis.rules.SpringApiRule;
import com.modernizeai.analysis.rules.SpringBootVersionRule;
import com.modernizeai.model.Certainty;
import com.modernizeai.model.Finding;
import com.modernizeai.model.Severity;
import com.modernizeai.support.TestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RulesTest {

    private static final String BOOT2_POM = TestSupport.pom("2.7.18", "<java.version>11</java.version>", null);
    private static final String BOOT3_POM = TestSupport.pom("3.2.5", "<java.version>17</java.version>", null);

    private static List<Finding> run(Map<String, String> files, MigrationRule rule) {
        return TestSupport.scan(files, rule).findings();
    }

    @Nested
    class Jakarta {
        @Test
        void flagsMovedJavaxPackagesAsVerifiedWithLine() {
            String src = """
                    package a;

                    import java.util.List;
                    import javax.persistence.Entity;
                    import javax.servlet.http.HttpServletRequest;
                    import javax.validation.*;

                    @Entity class A {}
                    """;
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "src/main/java/a/A.java", src), new JakartaNamespaceRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-JAKARTA-001");
                assertThat(x.certainty()).isEqualTo(Certainty.VERIFIED);
                assertThat(x.severity()).isEqualTo(Severity.HIGH);
                assertThat(x.line()).isEqualTo(4);
                assertThat(x.evidence()).contains("javax.persistence.Entity", "javax.servlet.http.HttpServletRequest", "javax.validation.*");
                assertThat(x.suggestedNextStep()).contains("javax.persistence → jakarta.persistence");
                assertThat(x.snippet()).contains(">", "import javax.persistence.Entity;");
            });
        }

        @Test
        void ignoresJdkOwnedJavaxPackagesAndJsr305() {
            String src = """
                    package a;
                    import javax.sql.DataSource;
                    import javax.crypto.Cipher;
                    import javax.annotation.Nullable;
                    import javax.annotation.processing.Processor;
                    import javax.xml.parsers.DocumentBuilder;
                    class A {}
                    """;
            assertThat(run(Map.of("pom.xml", BOOT2_POM, "A.java", src), new JakartaNamespaceRule())).isEmpty();
        }

        @Test
        void flagsAmbiguousPackagesAsPotential() {
            String src = "package a;\nimport javax.annotation.PostConstruct;\nimport javax.inject.Inject;\nclass A {}\n";
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "A.java", src), new JakartaNamespaceRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-JAKARTA-002");
                assertThat(x.certainty()).isEqualTo(Certainty.POTENTIAL);
            });
        }

        @Test
        void stillWorksWhenFileDoesNotParse() {
            String src = "package a;\nimport javax.servlet.Filter;\nclass Broken { void x( { }\n";
            MigrationScanner.ScanResult r = TestSupport.scan(Map.of("pom.xml", BOOT2_POM, "Broken.java", src), new JakartaNamespaceRule());
            assertThat(r.findings()).extracting(Finding::ruleId).containsExactly("MIG-JAKARTA-001");
            assertThat(r.warnings()).anyMatch(w -> w.contains("Broken.java"));
        }
    }

    @Nested
    class SpringBoot {
        @Test
        void flagsBoot2AsVerifiedHigh() {
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM), new SpringBootVersionRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-BOOT-001");
                assertThat(x.certainty()).isEqualTo(Certainty.VERIFIED);
                assertThat(x.line()).isEqualTo(4);
                assertThat(x.evidence()).contains("2.7.18");
            });
        }

        @Test
        void recommendsGoingVia27WhenOlder() {
            List<Finding> f = run(Map.of("pom.xml", TestSupport.pom("2.3.12.RELEASE", null, null)), new SpringBootVersionRule());
            assertThat(f.get(0).suggestedNextStep()).contains("2.7.x first");
        }

        @Test
        void noMajorUpgradeFindingForBoot32() {
            assertThat(run(Map.of("pom.xml", BOOT3_POM), new SpringBootVersionRule())).isEmpty();
        }

        @Test
        void unknownVersionIsReportedAsInfoNotGuessed() {
            String pom = TestSupport.pom(null, null, TestSupport.dep("org.springframework.boot", "spring-boot-starter-web", null));
            List<Finding> f = run(Map.of("pom.xml", pom), new SpringBootVersionRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-BOOT-003");
                assertThat(x.severity()).isEqualTo(Severity.INFO);
            });
        }
    }

    @Nested
    class JavaVersion {
        @Test
        void flagsJavaBelowTarget() {
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM), new JavaVersionRule());
            assertThat(f).extracting(Finding::ruleId).containsExactly("MIG-JAVA-001");
            assertThat(f.get(0).title()).contains("Java 11").contains("17");
        }

        @Test
        void respectsTarget21() {
            MigrationScanner.ScanResult r = TestSupport.scanner(new JavaVersionRule())
                    .scan(TestSupport.files(Map.of("pom.xml", BOOT3_POM)), 21);
            assertThat(r.findings()).extracting(Finding::ruleId).containsExactly("MIG-JAVA-001");
        }

        @Test
        void noFindingWhenAtTarget() {
            assertThat(run(Map.of("pom.xml", BOOT3_POM), new JavaVersionRule())).isEmpty();
        }

        @Test
        void undeclaredVersionIsPotential() {
            List<Finding> f = run(Map.of("pom.xml", TestSupport.pom(null, null, null)), new JavaVersionRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-JAVA-003");
                assertThat(x.certainty()).isEqualTo(Certainty.POTENTIAL);
            });
        }

        @Test
        void sourceTargetMismatch() {
            String pom = TestSupport.pom(null, "<maven.compiler.source>1.8</maven.compiler.source><maven.compiler.target>11</maven.compiler.target>", null);
            assertThat(run(Map.of("pom.xml", pom), new JavaVersionRule())).extracting(Finding::ruleId)
                    .containsExactlyInAnyOrder("MIG-JAVA-001", "MIG-JAVA-004");
        }
    }

    @Nested
    class SpringApi {
        @Test
        void verifiedWhenSupertypeIsExplicitlyImported() {
            String src = """
                    package a;
                    import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
                    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
                    public class Sec extends WebSecurityConfigurerAdapter {
                      protected void configure(HttpSecurity http) throws Exception {
                        http.authorizeRequests().antMatchers("/a").permitAll().antMatchers("/b").authenticated();
                      }
                    }
                    """;
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "Sec.java", src), new SpringApiRule());
            assertThat(f).extracting(Finding::ruleId).containsExactlyInAnyOrder("MIG-SPRING-001", "MIG-SPRING-003", "MIG-SPRING-004");
            Finding adapter = f.stream().filter(x -> x.ruleId().equals("MIG-SPRING-001")).findFirst().orElseThrow();
            assertThat(adapter.certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(adapter.line()).isEqualTo(4);
            Finding matchers = f.stream().filter(x -> x.ruleId().equals("MIG-SPRING-003")).findFirst().orElseThrow();
            assertThat(matchers.certainty()).isEqualTo(Certainty.POTENTIAL);
            assertThat(matchers.evidence()).startsWith("2 call(s)");
        }

        @Test
        void potentialWhenOnlyWildcardImport() {
            String src = """
                    package a;
                    import org.springframework.web.servlet.config.annotation.*;
                    public class W extends WebMvcConfigurerAdapter {}
                    """;
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "W.java", src), new SpringApiRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-SPRING-005");
                assertThat(x.certainty()).isEqualTo(Certainty.POTENTIAL);
            });
        }

        @Test
        void ignoresSameSimpleNameFromOtherPackage() {
            String src = "package a;\nimport com.mycorp.WebSecurityConfigurerAdapter;\nclass S extends WebSecurityConfigurerAdapter {}\n";
            assertThat(run(Map.of("pom.xml", BOOT2_POM, "S.java", src), new SpringApiRule())).isEmpty();
        }

        @Test
        void methodNameWithoutSpringSecurityImportIsIgnored() {
            String src = "package a;\nclass M { void x(Foo f) { f.antMatchers(\"/x\"); } }\n";
            assertThat(run(Map.of("pom.xml", BOOT2_POM, "M.java", src), new SpringApiRule())).isEmpty();
        }

        @Test
        void detectsAnnotationAndMovedImports() {
            String src = """
                    package a;
                    import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
                    import org.springframework.boot.web.server.LocalServerPort;
                    @EnableGlobalMethodSecurity(prePostEnabled = true)
                    class C { @LocalServerPort int port; }
                    """;
            assertThat(run(Map.of("pom.xml", BOOT2_POM, "C.java", src), new SpringApiRule())).extracting(Finding::ruleId)
                    .containsExactlyInAnyOrder("MIG-SPRING-002", "MIG-SPRING-007");
        }
    }

    @Nested
    class JdkInternal {
        @Test
        void flagsRemovedBase64AsVerified() {
            String src = "package a;\nimport sun.misc.BASE64Encoder;\nimport sun.misc.Unsafe;\nclass E {}\n";
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "E.java", src), new JdkInternalApiRule());
            assertThat(f).hasSize(2);
            assertThat(f.get(0).certainty()).isEqualTo(Certainty.VERIFIED);
            assertThat(f.get(1).certainty()).isEqualTo(Certainty.POTENTIAL);
        }
    }

    @Nested
    class Configuration {
        @Test
        void flagsRenamedPropertiesInPropertiesFile() {
            String props = "# spring.redis.host=commented\nspring.redis.host=localhost\nserver.port=8080\nserver.max-http-header-size=8KB\n";
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "src/main/resources/application.properties", props),
                    new ConfigurationPropertiesRule());
            assertThat(f).extracting(Finding::line).containsExactlyInAnyOrder(2, 4);
            assertThat(f).allMatch(x -> x.ruleId().equals("MIG-CONFIG-001"));
            assertThat(f.stream().filter(x -> x.line() == 2).findFirst().orElseThrow().suggestedNextStep())
                    .contains("spring.data.redis.host");
        }

        @Test
        void flattensYamlKeys() {
            String yml = """
                    spring:
                      application:
                        name: x
                      redis:
                        host: localhost
                    management:
                      metrics:
                        export:
                          prometheus:
                            enabled: true
                    """;
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "src/main/resources/application.yml", yml), new ConfigurationPropertiesRule());
            assertThat(f).extracting(Finding::evidence)
                    .containsExactlyInAnyOrder("line 5: spring.redis.host", "line 10: management.metrics.export.prometheus.enabled");
        }

        @Test
        void modernYamlProducesNoFindings() {
            String yml = "spring:\n  data:\n    redis:\n      host: localhost\n";
            assertThat(run(Map.of("pom.xml", BOOT3_POM, "src/main/resources/application.yml", yml), new ConfigurationPropertiesRule())).isEmpty();
        }

        @Test
        void flagsSpringFactoriesAutoConfiguration() {
            String factories = "org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\\n  a.B\n";
            List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "src/main/resources/META-INF/spring.factories", factories),
                    new ConfigurationPropertiesRule());
            assertThat(f).extracting(Finding::ruleId).containsExactly("MIG-CONFIG-002");
        }
    }

    @Nested
    class Dependencies {
        @Test
        void conflictingDuplicatesAreVerifiedHigh() {
            String pom = TestSupport.pom(null, null, TestSupport.dep("org.apache.commons", "commons-lang3", "3.9")
                    + TestSupport.dep("org.apache.commons", "commons-lang3", "3.12.0"));
            List<Finding> f = run(Map.of("pom.xml", pom), new DependencyRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-DEP-001");
                assertThat(x.severity()).isEqualTo(Severity.HIGH);
                assertThat(x.evidence()).contains("3.9", "3.12.0");
            });
        }

        @Test
        void missingVersionWithoutParentIsVerified() {
            String pom = TestSupport.pom(null, null, TestSupport.dep("com.google.guava", "guava", null));
            List<Finding> f = run(Map.of("pom.xml", pom), new DependencyRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-DEP-002");
                assertThat(x.certainty()).isEqualTo(Certainty.VERIFIED);
                assertThat(x.severity()).isEqualTo(Severity.HIGH);
            });
        }

        @Test
        void bootManagedDependenciesWithoutVersionAreFine() {
            String pom = TestSupport.pom("3.2.5", null, TestSupport.dep("org.springframework.boot", "spring-boot-starter-web", null)
                    + TestSupport.dep("com.fasterxml.jackson.core", "jackson-databind", null));
            assertThat(run(Map.of("pom.xml", pom), new DependencyRule())).isEmpty();
        }

        @Test
        void explicitOverrideOfManagedVersionIsPotential() {
            String pom = TestSupport.pom("2.7.18", null, TestSupport.dep("com.fasterxml.jackson.core", "jackson-databind", "2.11.0"));
            List<Finding> f = run(Map.of("pom.xml", pom), new DependencyRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-DEP-003");
                assertThat(x.certainty()).isEqualTo(Certainty.POTENTIAL);
            });
        }

        @Test
        void knownIncompatibleArtifacts() {
            String pom = TestSupport.pom("2.7.18", "<sf>3.0.0</sf>", TestSupport.dep("io.springfox", "springfox-boot-starter", "${sf}")
                    + TestSupport.dep("javax.servlet", "javax.servlet-api", "4.0.1")
                    + TestSupport.dep("mysql", "mysql-connector-java", null));
            List<Finding> f = run(Map.of("pom.xml", pom), new DependencyRule());
            assertThat(f).filteredOn(x -> x.ruleId().equals("MIG-DEP-004")).hasSize(3);
        }

        @Test
        void undefinedPropertyIsVerifiedWithoutParent() {
            String pom = TestSupport.pom(null, null, TestSupport.dep("com.x", "y", "${nope}"));
            List<Finding> f = run(Map.of("pom.xml", pom), new DependencyRule());
            assertThat(f).singleElement().satisfies(x -> {
                assertThat(x.ruleId()).isEqualTo("MIG-DEP-005");
                assertThat(x.certainty()).isEqualTo(Certainty.VERIFIED);
            });
        }

        @Test
        void mixedBootArtifactVersions() {
            String pom = TestSupport.pom("2.7.18", null, TestSupport.dep("org.springframework.boot", "spring-boot-starter-web", "2.5.0"));
            assertThat(run(Map.of("pom.xml", pom), new DependencyRule())).extracting(Finding::ruleId).contains("MIG-DEP-006");
        }
    }

    @Test
    void malformedPomBecomesFindingAndAnalysisContinues() {
        String src = "package a;\nimport javax.servlet.Filter;\nclass A {}\n";
        MigrationScanner.ScanResult r = TestSupport.scan(Map.of("pom.xml", "<project><dependencies></project>", "A.java", src),
                new PomErrorRule(), new JakartaNamespaceRule());
        assertThat(r.findings()).extracting(Finding::ruleId).containsExactlyInAnyOrder("MIG-POM-001", "MIG-JAKARTA-001");
        assertThat(r.findings()).filteredOn(f -> f.ruleId().equals("MIG-POM-001")).singleElement()
                .satisfies(f -> assertThat(f.line()).isEqualTo(1));
    }

    @Test
    void scannerAssignsSequentialIdsSortedBySeverity() {
        String src = "package a;\nimport javax.annotation.PostConstruct;\nimport javax.servlet.Filter;\nclass A {}\n";
        List<Finding> f = run(Map.of("pom.xml", BOOT2_POM, "A.java", src), new JakartaNamespaceRule());
        assertThat(f).extracting(Finding::id).containsExactly("F-001", "F-002");
        assertThat(f.get(0).severity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void faultyRuleDoesNotAbortScan() {
        MigrationRule boom = new MigrationRule() {
            public String name() { return "boom"; }
            public List<Finding> evaluate(ProjectContext c) { throw new IllegalStateException("bug"); }
        };
        MigrationScanner.ScanResult r = TestSupport.scan(Map.of("pom.xml", BOOT2_POM), boom, new SpringBootVersionRule());
        assertThat(r.findings()).extracting(Finding::ruleId).containsExactly("MIG-BOOT-001");
        assertThat(r.warnings()).anyMatch(w -> w.contains("boom"));
    }
}
