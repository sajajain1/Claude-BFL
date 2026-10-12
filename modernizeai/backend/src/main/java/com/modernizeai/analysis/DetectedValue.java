package com.modernizeai.analysis;

/**
 * A detected fact plus its provenance.
 *
 * @param value    normalised value, e.g. "8" or "2.7.18"
 * @param raw      text as written in the file, e.g. "1.8"
 * @param source   human-readable description of where it came from
 * @param path     file it was found in
 * @param line     line in that file, if known
 * @param inferred true when not declared explicitly (e.g. a parent default)
 */
public record DetectedValue(String value, String raw, String source, String path, Integer line, boolean inferred) {}
