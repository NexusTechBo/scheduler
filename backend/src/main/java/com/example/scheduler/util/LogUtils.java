package com.example.scheduler.util;

import java.util.Objects;
import java.util.regex.Pattern;

public final class LogUtils {
    private static final String MASK = "***";
    private static final String MISSING_VALUE = "-";
    private static final int MAX_LOGGED_USERNAME = 100;
    private static final Pattern EMAIL_IN_TEXT =
            Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+)");
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]+\\s*");

    private LogUtils() {}

    public static String maskEmail(String email) {
        var at = email.indexOf('@');
        if (at <= 0) return MASK;
        return email.charAt(0) + MASK + email.substring(at);
    }

    public static String sanitizeForLog(String text) {
        if (text == null) return "";
        var masked = EMAIL_IN_TEXT.matcher(text).replaceAll("$1***@$2");
        return LINE_BREAKS.matcher(masked).replaceAll(" ").strip();
    }

    public static String sanitizeUsername(String username) {
        var clean = Utils.stripControlChars(username);
        return clean.length() > MAX_LOGGED_USERNAME ? clean.substring(0, MAX_LOGGED_USERNAME) + "..." : clean;
    }

    public static String orDash(Object value) {
        return Objects.toString(value, MISSING_VALUE);
    }
}
