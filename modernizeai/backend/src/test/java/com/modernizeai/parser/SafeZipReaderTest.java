package com.modernizeai.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.exception.InvalidUploadException;
import com.modernizeai.exception.UnsafeArchiveException;
import com.modernizeai.support.TestSupport;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SafeZipReaderTest {

    private final SafeZipReader reader = new SafeZipReader(TestSupport.defaultProperties());

    @Test
    void readsRelevantFilesAndSkipsOthers() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("demo/", null);
        entries.put("demo/pom.xml", "<project/>");
        entries.put("demo/src/main/java/A.java", "class A {}");
        entries.put("demo/src/main/resources/application.properties", "a=b");
        entries.put("demo/README.md", "# readme");
        entries.put("demo/target/classes/B.java", "class B {}");
        entries.put("__MACOSX/demo/._pom.xml", "junk");

        ProjectFiles files = reader.read(TestSupport.zip(entries));

        assertThat(files.all()).containsOnlyKeys("demo/pom.xml", "demo/src/main/java/A.java",
                "demo/src/main/resources/application.properties");
        assertThat(files.rootPomPath()).contains("demo/pom.xml");
        assertThat(files.skippedFiles()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../evil.java", "demo/../../evil.java", "/etc/passwd.java", "C:/windows/evil.java",
            "demo\\..\\evil.java", "demo/./../../x/pom.xml"})
    void rejectsPathTraversalAndAbsolutePaths(String name) {
        byte[] zip = TestSupport.zip(Map.of("demo/pom.xml", "<project/>", name, "x"));
        assertThatThrownBy(() -> reader.read(zip))
                .isInstanceOf(UnsafeArchiveException.class)
                .hasMessageContaining("Unsafe path");
    }

    @Test
    void normalisesHarmlessDotSegments() {
        assertThat(SafeZipReader.validateEntryName("./demo//src/./A.java")).isEqualTo("demo/src/A.java");
    }

    @Test
    void rejectsNonZipContent() {
        assertThatThrownBy(() -> reader.read("this is not a zip".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("not a ZIP");
    }

    @Test
    void rejectsEmptyInput() {
        assertThatThrownBy(() -> reader.read(new byte[0])).isInstanceOf(InvalidUploadException.class);
        assertThatThrownBy(() -> reader.read(TestSupport.zip(Map.of())))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsCorruptZip() {
        byte[] zip = TestSupport.zip(Map.of("pom.xml", "<project>" + "x".repeat(5000) + "</project>"));
        byte[] truncated = java.util.Arrays.copyOf(zip, 60);
        assertThatThrownBy(() -> reader.read(truncated)).isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void rejectsTooManyEntries() {
        SafeZipReader strict = new SafeZipReader(TestSupport.propertiesWithUpload(
                new ModernizeAiProperties.Upload(1024 * 1024, 5, 10 * 1024 * 1024, 1024 * 1024, 100)));
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) entries.put("f" + i + ".java", "class F" + i + " {}");
        assertThatThrownBy(() -> strict.read(TestSupport.zip(entries)))
                .isInstanceOf(UnsafeArchiveException.class)
                .hasMessageContaining("more than 5 entries");
    }

    @Test
    void rejectsZipBombByTotalUncompressedSize() {
        SafeZipReader strict = new SafeZipReader(TestSupport.propertiesWithUpload(
                new ModernizeAiProperties.Upload(10 * 1024 * 1024, 100, 2 * 1024 * 1024, 1024 * 1024, 1000)));
        // ~5 MB of zeros compresses to a few KB.
        byte[] zip = TestSupport.zip(Map.of("bomb.txt", "0".repeat(5 * 1024 * 1024)));
        assertThatThrownBy(() -> strict.read(zip))
                .isInstanceOf(UnsafeArchiveException.class);
    }

    @Test
    void rejectsZipBombByCompressionRatio() {
        // Default ratio limit is 100; 20 MB of zeros compresses roughly 1000:1.
        byte[] zip = TestSupport.zip(Map.of("bomb.txt", "0".repeat(20 * 1024 * 1024)));
        assertThatThrownBy(() -> reader.read(zip))
                .isInstanceOf(UnsafeArchiveException.class)
                .hasMessageContaining("compression ratio");
    }

    @Test
    void skipsOversizedSourceFileWithWarning() {
        SafeZipReader strict = new SafeZipReader(TestSupport.propertiesWithUpload(
                new ModernizeAiProperties.Upload(1024 * 1024, 100, 10 * 1024 * 1024, 1024, 10_000)));
        byte[] zip = TestSupport.zip(Map.of("pom.xml", "<project/>", "Big.java", "x".repeat(4096)));
        ProjectFiles files = strict.read(zip);
        assertThat(files.all()).containsOnlyKeys("pom.xml");
        assertThat(files.warnings()).anyMatch(w -> w.contains("Big.java"));
    }

    @Test
    void rejectsUploadsAboveSizeLimit() {
        SafeZipReader strict = new SafeZipReader(TestSupport.propertiesWithUpload(
                new ModernizeAiProperties.Upload(1024, 100, 10 * 1024 * 1024, 1024 * 1024, 100)));
        byte[] big = new byte[2048];
        big[0] = 'P'; big[1] = 'K'; big[2] = 3; big[3] = 4;
        assertThatThrownBy(() -> strict.read(big)).isInstanceOf(InvalidUploadException.class).hasMessageContaining("limit");
    }
}
