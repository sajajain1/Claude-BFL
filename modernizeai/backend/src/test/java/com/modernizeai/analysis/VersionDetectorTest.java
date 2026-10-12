package com.modernizeai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.modernizeai.parser.PomModel;
import com.modernizeai.parser.PomParser;
import com.modernizeai.support.TestSupport;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VersionDetectorTest {

    private final PomParser parser = new PomParser();
    private final VersionDetector detector = new VersionDetector();

    private PomModel pom(String xml) throws Exception {
        return parser.parse("pom.xml", xml);
    }

    @ParameterizedTest
    @CsvSource({"1.8,8", "8,8", "11,11", "17,17", "21,21", "1.7,7", "17.0.2,17"})
    void normalisesJavaVersions(String raw, int expected) {
        assertThat(VersionUtils.javaMajor(raw)).isEqualTo(expected);
    }

    @Test
    void javaVersionFromJavaVersionProperty() throws Exception {
        PomModel p = pom(TestSupport.pom("2.7.18", "<java.version>1.8</java.version>", null));
        Optional<DetectedValue> v = detector.javaVersion(p, detector.springBootVersion(p));
        assertThat(v).isPresent();
        assertThat(v.get().value()).isEqualTo("8");
        assertThat(v.get().raw()).isEqualTo("1.8");
        assertThat(v.get().source()).contains("java.version");
        assertThat(v.get().inferred()).isFalse();
    }

    @Test
    void compilerReleasePropertyWinsOverJavaVersion() throws Exception {
        PomModel p = pom(TestSupport.pom(null, "<java.version>8</java.version><maven.compiler.release>11</maven.compiler.release>", null));
        assertThat(detector.javaVersion(p, Optional.empty()).get().value()).isEqualTo("11");
    }

    @Test
    void compilerPluginConfigurationWinsOverProperties() throws Exception {
        String xml = """
                <project>
                  <properties><maven.compiler.source>17</maven.compiler.source></properties>
                  <build><plugins><plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration><source>1.8</source><target>1.8</target></configuration>
                  </plugin></plugins></build>
                </project>""";
        DetectedValue v = detector.javaVersion(pom(xml), Optional.empty()).get();
        assertThat(v.value()).isEqualTo("8");
        assertThat(v.source()).contains("maven-compiler-plugin");
        assertThat(v.line()).isEqualTo(6);
    }

    @Test
    void javaVersionInferredFromBootParentDefaultWhenUndeclared() throws Exception {
        PomModel p = pom(TestSupport.pom("2.3.12.RELEASE", null, null));
        DetectedValue v = detector.javaVersion(p, detector.springBootVersion(p)).get();
        assertThat(v.value()).isEqualTo("8");
        assertThat(v.inferred()).isTrue();
    }

    @Test
    void javaVersionAbsentWithoutAnyHint() throws Exception {
        assertThat(detector.javaVersion(pom(TestSupport.pom(null, null, null)), Optional.empty())).isEmpty();
    }

    @Test
    void springBootFromParent() throws Exception {
        DetectedValue v = detector.springBootVersion(pom(TestSupport.pom("2.7.18", null, null))).get();
        assertThat(v.value()).isEqualTo("2.7.18");
        assertThat(v.source()).contains("spring-boot-starter-parent");
    }

    @Test
    void springBootFromBomImportWithProperty() throws Exception {
        String xml = """
                <project>
                  <properties><spring-boot.version>2.6.15</spring-boot.version></properties>
                  <dependencyManagement><dependencies><dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-dependencies</artifactId>
                    <version>${spring-boot.version}</version>
                    <type>pom</type><scope>import</scope>
                  </dependency></dependencies></dependencyManagement>
                </project>""";
        DetectedValue v = detector.springBootVersion(pom(xml)).get();
        assertThat(v.value()).isEqualTo("2.6.15");
        assertThat(v.source()).contains("BOM");
    }

    @Test
    void springBootAbsentForPlainProject() throws Exception {
        assertThat(detector.springBootVersion(pom(TestSupport.pom(null, null,
                TestSupport.dep("com.google.guava", "guava", "33.0.0-jre"))))).isEmpty();
    }

    @Test
    void versionComparison() {
        assertThat(VersionUtils.compare("3.1.5", "3.2.0")).isNegative();
        assertThat(VersionUtils.compare("3.2.0", "3.2")).isZero();
        assertThat(VersionUtils.major("2.7.18")).isEqualTo(2);
        assertThat(VersionUtils.minor("2.7.18")).isEqualTo(7);
        assertThat(VersionUtils.major("${x}")).isNull();
    }
}
