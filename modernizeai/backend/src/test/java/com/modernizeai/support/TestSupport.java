package com.modernizeai.support;

import com.modernizeai.analysis.JavaSourceParser;
import com.modernizeai.analysis.MigrationRule;
import com.modernizeai.analysis.MigrationScanner;
import com.modernizeai.analysis.VersionDetector;
import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.parser.PomParser;
import com.modernizeai.parser.ProjectFiles;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class TestSupport {

    /** The demo projects live in modernizeai/samples, next to the backend module. */
    public static final Path SAMPLES = Path.of("..", "samples");

    private TestSupport() {}

    public static ModernizeAiProperties defaultProperties() {
        return new ModernizeAiProperties(null, null, null, null);
    }

    public static ModernizeAiProperties propertiesWithUpload(ModernizeAiProperties.Upload upload) {
        return new ModernizeAiProperties(upload, null, null, null);
    }

    public static byte[] zip(Map<String, String> entries) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                if (e.getValue() != null) zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static byte[] zipDirectory(Path dir) {
        Map<String, String> entries = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.filter(Files::isRegularFile).sorted().toList()) {
                entries.put(dir.getFileName() + "/" + dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return zip(entries);
    }

    public static ProjectFiles files(Map<String, String> files) {
        return new ProjectFiles(files, files.size(), 0, List.of());
    }

    public static MigrationScanner scanner(MigrationRule... rules) {
        return new MigrationScanner(new PomParser(), new JavaSourceParser(), new VersionDetector(), List.of(rules));
    }

    public static MigrationScanner.ScanResult scan(Map<String, String> files, MigrationRule... rules) {
        return scanner(rules).scan(files(files), 17);
    }

    public static String pom(String parentVersion, String properties, String dependencies) {
        String parent = parentVersion == null ? "" : """
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>%s</version>
                  </parent>
                """.formatted(parentVersion);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                %s  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <properties>%s</properties>
                  <dependencies>%s</dependencies>
                </project>
                """.formatted(parent, properties == null ? "" : properties, dependencies == null ? "" : dependencies);
    }

    public static String dep(String groupId, String artifactId, String version) {
        return "<dependency><groupId>" + groupId + "</groupId><artifactId>" + artifactId + "</artifactId>"
                + (version == null ? "" : "<version>" + version + "</version>") + "</dependency>\n";
    }
}
