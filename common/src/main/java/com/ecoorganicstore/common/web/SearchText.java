package com.ecoorganicstore.common.web;

import java.util.regex.Pattern;

public final class SearchText {
    public static final int MAX_LENGTH = 80;
    private static final Pattern META = Pattern.compile("[\\\\.^$|?*+()\\[\\]{}]");

    private SearchText() {}

    public static String clip(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.length() <= MAX_LENGTH) return trimmed;
        return trimmed.substring(0, MAX_LENGTH);
    }

    public static String literal(String raw) {
        String clipped = clip(raw);
        if (clipped.isEmpty()) return "";
        return META.matcher(clipped).replaceAll("\\\\$0");
    }
}
