package com.modernizeai.analysis;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Best-effort masking of credentials in snippets before they are stored, displayed, exported or
 * sent to Claude. It is a safety net, not a guarantee: users should not upload secrets.
 */
public final class SecretRedactor {

    public static final String MASK = "***REDACTED***";

    private static final List<Pattern> KEY_VALUE = List.of(
            Pattern.compile("(?i)((?:password|passwd|pwd|secret|api[_-]?key|access[_-]?key|auth[_-]?token|token|credentials?|private[_-]?key)[\\w.-]*\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,;]+)"));
    private static final List<Pattern> RAW = List.of(
            Pattern.compile("sk-ant-[A-Za-z0-9_-]{8,}"),
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            Pattern.compile("gh[pousr]_[A-Za-z0-9]{20,}"),
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?(-----END [A-Z ]*PRIVATE KEY-----|$)"),
            Pattern.compile("(?i)(jdbc:[a-z0-9]+://)([^:/@\\s]+):([^@\\s]+)@"));

    private SecretRedactor() {}

    public static String redact(String text) {
        if (text == null) return null;
        String out = text;
        for (Pattern p : KEY_VALUE) {
            out = p.matcher(out).replaceAll(m -> java.util.regex.Matcher.quoteReplacement(m.group(1) + MASK));
        }
        out = RAW.get(0).matcher(out).replaceAll(MASK);
        out = RAW.get(1).matcher(out).replaceAll(MASK);
        out = RAW.get(2).matcher(out).replaceAll(MASK);
        out = RAW.get(3).matcher(out).replaceAll(MASK);
        out = RAW.get(4).matcher(out).replaceAll(m -> java.util.regex.Matcher.quoteReplacement(m.group(1) + MASK + "@"));
        return out;
    }
}
