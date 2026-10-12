package com.modernizeai.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory view of the text files from an uploaded project that are relevant to analysis.
 * Nothing is written to disk. The object is discarded after analysis.
 */
public final class ProjectFiles {

    private final Map<String, String> files;
    private final int totalEntries;
    private final int skippedFiles;
    private final List<String> warnings;

    public ProjectFiles(Map<String, String> files, int totalEntries, int skippedFiles, List<String> warnings) {
        this.files = Collections.unmodifiableMap(new TreeMap<>(files));
        this.totalEntries = totalEntries;
        this.skippedFiles = skippedFiles;
        this.warnings = List.copyOf(warnings);
    }

    public Map<String, String> all() {
        return files;
    }

    public Optional<String> get(String path) {
        return Optional.ofNullable(files.get(path));
    }

    public List<String> pathsEndingWith(String suffix) {
        return files.keySet().stream().filter(p -> p.endsWith(suffix)).toList();
    }

    public List<String> pomPaths() {
        return files.keySet().stream().filter(p -> p.equals("pom.xml") || p.endsWith("/pom.xml")).toList();
    }

    /** The pom.xml closest to the archive root, which is treated as the project's root POM. */
    public Optional<String> rootPomPath() {
        return pomPaths().stream().min((a, b) -> {
            int d = Long.compare(a.chars().filter(c -> c == '/').count(), b.chars().filter(c -> c == '/').count());
            return d != 0 ? d : a.compareTo(b);
        });
    }

    public int totalEntries() {
        return totalEntries;
    }

    public int skippedFiles() {
        return skippedFiles;
    }

    public List<String> warnings() {
        return warnings;
    }
}
