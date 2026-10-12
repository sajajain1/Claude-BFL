package com.modernizeai.parser;

import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.exception.InvalidUploadException;
import com.modernizeai.exception.UnsafeArchiveException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

/**
 * Reads a ZIP archive entirely in memory, keeping only the text files the scanner needs.
 *
 * <p>Safety properties:
 * <ul>
 *   <li>Nothing is written to the filesystem, so path traversal cannot write files. Archives
 *       containing unsafe entry names are still rejected outright, because they signal a hostile
 *       or broken upload.</li>
 *   <li>Entry count, total decompressed bytes and overall compression ratio are bounded while
 *       streaming, so zip bombs are stopped before they exhaust memory.</li>
 *   <li>Uploaded content is never executed or interpreted beyond text parsing.</li>
 * </ul>
 */
@Component
public class SafeZipReader {

    private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};
    private static final byte[] EMPTY_ZIP_MAGIC = {'P', 'K', 5, 6};
    private static final Pattern WINDOWS_DRIVE = Pattern.compile("^[A-Za-z]:.*");
    private static final Set<String> IGNORED_DIRS = Set.of("__MACOSX", ".git", "target", "node_modules", ".idea", "build", ".gradle");

    private final ModernizeAiProperties.Upload limits;

    public SafeZipReader(ModernizeAiProperties properties) {
        this.limits = properties.upload();
    }

    public ProjectFiles read(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new InvalidUploadException("The uploaded file is empty.");
        }
        if (zipBytes.length > limits.maxZipBytes()) {
            throw new InvalidUploadException("The uploaded file exceeds the " + (limits.maxZipBytes() / (1024 * 1024)) + " MB limit.");
        }
        if (startsWith(zipBytes, EMPTY_ZIP_MAGIC)) {
            throw new InvalidUploadException("The ZIP archive is empty.");
        }
        if (!startsWith(zipBytes, ZIP_MAGIC)) {
            throw new InvalidUploadException("The uploaded file is not a ZIP archive.");
        }

        Map<String, String> files = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        int entries = 0;
        int skipped = 0;
        long totalBytes = 0;
        byte[] buffer = new byte[8192];

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entries > limits.maxEntries()) {
                    throw new UnsafeArchiveException("The archive contains more than " + limits.maxEntries() + " entries.");
                }
                String name = validateEntryName(entry.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                boolean wanted = isRelevant(name);
                ByteArrayOutputStream out = wanted ? new ByteArrayOutputStream() : null;
                long entryBytes = 0;
                boolean tooBig = false;
                int n;
                while ((n = zis.read(buffer)) > 0) {
                    entryBytes += n;
                    totalBytes += n;
                    if (totalBytes > limits.maxTotalUncompressedBytes()) {
                        throw new UnsafeArchiveException("The archive expands beyond the "
                                + (limits.maxTotalUncompressedBytes() / (1024 * 1024)) + " MB extraction limit.");
                    }
                    if (totalBytes > (long) zipBytes.length * limits.maxCompressionRatio() && totalBytes > 1024 * 1024) {
                        throw new UnsafeArchiveException("The archive's compression ratio is suspiciously high (possible zip bomb).");
                    }
                    if (out != null) {
                        if (entryBytes > limits.maxFileBytes()) {
                            tooBig = true;
                            out = null;
                        } else {
                            out.write(buffer, 0, n);
                        }
                    }
                }
                if (!wanted) {
                    skipped++;
                } else if (tooBig) {
                    skipped++;
                    warnings.add("Skipped " + name + ": larger than " + (limits.maxFileBytes() / 1024) + " KB.");
                } else {
                    String text = decode(out.toByteArray());
                    if (text == null) {
                        skipped++;
                        warnings.add("Skipped " + name + ": not valid UTF-8 text.");
                    } else if (files.putIfAbsent(name, text) != null) {
                        throw new UnsafeArchiveException("The archive contains duplicate entries for '" + name + "'.");
                    }
                }
            }
        } catch (ZipException e) {
            throw new InvalidUploadException("The ZIP archive is corrupt or uses an unsupported format.");
        } catch (IOException e) {
            throw new InvalidUploadException("The ZIP archive could not be read.");
        } catch (IllegalArgumentException e) {
            // ZipInputStream throws this for malformed entry names (e.g. invalid encodings).
            throw new UnsafeArchiveException("The archive contains an entry with an invalid name.");
        }
        if (entries == 0) {
            throw new InvalidUploadException("The ZIP archive is empty or corrupt.");
        }
        return new ProjectFiles(files, entries, skipped, warnings);
    }

    /**
     * Rejects absolute paths, drive letters, backslashes, NUL bytes and any ".." segment. Returns
     * the normalised relative path.
     */
    static String validateEntryName(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new UnsafeArchiveException("The archive contains an entry with an empty name.");
        }
        if (raw.indexOf('\0') >= 0 || raw.indexOf('\\') >= 0) {
            throw new UnsafeArchiveException("Unsafe path in archive: entry names must not contain backslashes or NUL characters.");
        }
        if (raw.startsWith("/") || WINDOWS_DRIVE.matcher(raw).matches()) {
            throw new UnsafeArchiveException("Unsafe path in archive: absolute path '" + abbreviate(raw) + "'.");
        }
        String[] segments = raw.split("/");
        for (String s : segments) {
            if (s.equals("..")) {
                throw new UnsafeArchiveException("Unsafe path in archive: '" + abbreviate(raw) + "' attempts directory traversal.");
            }
        }
        if (raw.length() > 1024) {
            throw new UnsafeArchiveException("Unsafe path in archive: entry name too long.");
        }
        // Collapse "./" segments; there are no ".." segments left at this point.
        StringBuilder sb = new StringBuilder();
        for (String s : segments) {
            if (s.isEmpty() || s.equals(".")) continue;
            if (sb.length() > 0) sb.append('/');
            sb.append(s);
        }
        return sb.toString();
    }

    private static boolean isRelevant(String path) {
        for (String segment : path.split("/")) {
            if (IGNORED_DIRS.contains(segment)) return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        String file = lower.substring(lower.lastIndexOf('/') + 1);
        return file.equals("pom.xml")
                || lower.endsWith(".java")
                || (lower.contains("src/main/resources") && (lower.endsWith(".properties") || lower.endsWith(".yml") || lower.endsWith(".yaml")))
                || file.equals("spring.factories")
                || file.endsWith("autoconfiguration.imports");
    }

    private static String decode(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            String s = decoder.decode(ByteBuffer.wrap(bytes)).toString();
            return s.startsWith("﻿") ? s.substring(1) : s;
        } catch (CharacterCodingException e) {
            // Legacy projects are often ISO-8859-1; every byte sequence is valid in it.
            return new String(bytes, StandardCharsets.ISO_8859_1).indexOf('\0') >= 0 ? null : new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static String abbreviate(String s) {
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    /** Convenience for callers holding a stream. */
    public ProjectFiles read(InputStream in) throws IOException {
        return read(in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, limits.maxZipBytes() + 1)));
    }
}
