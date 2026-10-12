package com.modernizeai.analysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtils {

    private static final Pattern NUMERIC = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");

    private VersionUtils() {}

    /** "1.8" -> 8, "11" -> 11, "17.0.2" -> 17. Returns null when not a recognisable Java version. */
    public static Integer javaMajor(String v) {
        if (v == null) return null;
        Matcher m = NUMERIC.matcher(v.trim());
        if (!m.matches()) return null;
        int first = Integer.parseInt(m.group(1));
        if (first == 1 && m.group(2) != null) return Integer.parseInt(m.group(2));
        return first;
    }

    /** Major component of a semantic-ish version ("2.7.18" -> 2). Null when unparseable. */
    public static Integer major(String v) {
        if (v == null) return null;
        Matcher m = NUMERIC.matcher(v.trim());
        return m.matches() ? Integer.parseInt(m.group(1)) : null;
    }

    public static Integer minor(String v) {
        if (v == null) return null;
        Matcher m = NUMERIC.matcher(v.trim());
        return m.matches() && m.group(2) != null ? Integer.parseInt(m.group(2)) : null;
    }

    /** Compares numerically by major/minor/patch; non-numeric versions compare as equal (0). */
    public static int compare(String a, String b) {
        Matcher ma = NUMERIC.matcher(a == null ? "" : a.trim());
        Matcher mb = NUMERIC.matcher(b == null ? "" : b.trim());
        if (!ma.matches() || !mb.matches()) return 0;
        for (int i = 1; i <= 3; i++) {
            int x = ma.group(i) == null ? 0 : Integer.parseInt(ma.group(i));
            int y = mb.group(i) == null ? 0 : Integer.parseInt(mb.group(i));
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }
}
