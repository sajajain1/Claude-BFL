package com.modernizeai.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.modernizeai.model.DependencyInfo;
import org.junit.jupiter.api.Test;

class PomParserTest {

    private final PomParser parser = new PomParser();

    private static final String POM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.7.18</version>
              </parent>
              <groupId>com.acme</groupId>
              <artifactId>orders</artifactId>
              <version>3.1.0</version>
              <properties>
                <java.version>11</java.version>
                <lib.version>1.2.3</lib.version>
                <lib.alias>${lib.version}</lib.alias>
              </properties>
              <dependencies>
                <dependency>
                  <groupId>com.acme</groupId>
                  <artifactId>lib</artifactId>
                  <version>${lib.alias}</version>
                </dependency>
                <dependency>
                  <groupId>com.acme</groupId>
                  <artifactId>common</artifactId>
                  <version>${project.version}</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>com.acme</groupId>
                  <artifactId>missing</artifactId>
                  <version>${not.defined}</version>
                </dependency>
              </dependencies>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.cloud</groupId>
                    <artifactId>spring-cloud-dependencies</artifactId>
                    <version>2021.0.8</version>
                    <type>pom</type>
                    <scope>import</scope>
                  </dependency>
                </dependencies>
              </dependencyManagement>
              <build>
                <plugins>
                  <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                      <release>${java.version}</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """;

    @Test
    void parsesCoordinatesParentAndProperties() throws Exception {
        PomModel pom = parser.parse("pom.xml", POM);
        assertThat(pom.groupId()).isEqualTo("com.acme");
        assertThat(pom.artifactId()).isEqualTo("orders");
        assertThat(pom.version()).isEqualTo("3.1.0");
        assertThat(pom.hasSpringBootParent()).isTrue();
        assertThat(pom.parent().version()).isEqualTo("2.7.18");
        assertThat(pom.parent().line()).isEqualTo(4);
        assertThat(pom.property("java.version")).isEqualTo("11");
        assertThat(pom.properties().get("java.version").line()).isEqualTo(13);
    }

    @Test
    void resolvesPropertiesRecursivelyAndRecordsLines() throws Exception {
        PomModel pom = parser.parse("pom.xml", POM);
        DependencyInfo lib = pom.dependencies().get(0);
        assertThat(lib.declaredVersion()).isEqualTo("${lib.alias}");
        assertThat(lib.resolvedVersion()).isEqualTo("1.2.3");
        assertThat(lib.line()).isEqualTo(18);
        assertThat(pom.dependencies().get(1).resolvedVersion()).isEqualTo("3.1.0");
        assertThat(pom.dependencies().get(1).scope()).isEqualTo("test");
        assertThat(pom.dependencies().get(2).resolvedVersion()).isNull();
    }

    @Test
    void readsDependencyManagementAndPlugins() throws Exception {
        PomModel pom = parser.parse("pom.xml", POM);
        assertThat(pom.managedDependencies()).singleElement()
                .satisfies(d -> {
                    assertThat(d.scope()).isEqualTo("import");
                    assertThat(d.managed()).isTrue();
                });
        PomModel.Plugin compiler = pom.plugins().get(0);
        assertThat(compiler.groupId()).isEqualTo("org.apache.maven.plugins");
        assertThat(compiler.configuration().get("release").value()).isEqualTo("11");
    }

    @Test
    void reportsMalformedXmlWithLineNumber() {
        String broken = "<project>\n  <groupId>x</groupId>\n  <dependencies>\n</project>";
        assertThatThrownBy(() -> parser.parse("module/pom.xml", broken))
                .isInstanceOf(PomParseException.class)
                .hasMessageContaining("module/pom.xml is not well-formed XML")
                .satisfies(e -> assertThat(((PomParseException) e).line()).isEqualTo(4));
    }

    @Test
    void rejectsDoctypeToPreventXxe() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE project [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <project><groupId>&xxe;</groupId></project>
                """;
        assertThatThrownBy(() -> parser.parse("pom.xml", xxe)).isInstanceOf(PomParseException.class);
    }

    @Test
    void rejectsEmptyAndNonProjectDocuments() {
        assertThatThrownBy(() -> parser.parse("pom.xml", "  ")).isInstanceOf(PomParseException.class);
        assertThatThrownBy(() -> parser.parse("pom.xml", "<settings/>"))
                .isInstanceOf(PomParseException.class)
                .hasMessageContaining("<project>");
    }

    @Test
    void handlesPomWithoutNamespaceOrOptionalSections() throws Exception {
        PomModel pom = parser.parse("pom.xml", "<project><artifactId>bare</artifactId></project>");
        assertThat(pom.artifactId()).isEqualTo("bare");
        assertThat(pom.parent()).isNull();
        assertThat(pom.dependencies()).isEmpty();
        assertThat(pom.plugins()).isEmpty();
    }
}
