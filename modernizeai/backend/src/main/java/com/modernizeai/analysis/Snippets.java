package com.modernizeai.analysis;

/** Extracts a few numbered, redacted lines of context around a line. */
public final class Snippets {

    public static final int RADIUS = 3;
    public static final int MAX_CHARS = 1200;

    private Snippets() {}

    public static String around(String content, Integer line) {
        if (content == null || line == null || line < 1) return null;
        String[] lines = content.split("\\R", -1);
        if (line > lines.length) return null;
        int from = Math.max(1, line - RADIUS);
        int to = Math.min(lines.length, line + RADIUS);
        int width = String.valueOf(to).length();
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            String l = lines[i - 1];
            if (l.length() > 200) l = l.substring(0, 200) + " …";
            sb.append(i == line ? "> " : "  ")
                    .append(String.format("%" + width + "d", i))
                    .append(" | ").append(l).append('\n');
        }
        String s = SecretRedactor.redact(sb.toString());
        return s.length() > MAX_CHARS ? s.substring(0, MAX_CHARS) + "\n…(truncated)" : s;
    }

    public static String lineText(String content, Integer line) {
        if (content == null || line == null || line < 1) return null;
        String[] lines = content.split("\\R", -1);
        return line <= lines.length ? SecretRedactor.redact(lines[line - 1].trim()) : null;
    }
}
