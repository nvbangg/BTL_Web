package com.nvbangg.fashonshop.common.util;

import java.util.Collections;
import java.util.Locale;

public final class QueryUtils {

    private QueryUtils() {
    }

    public static int parsePositiveOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    public static String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String buildPlaceholders(int count) {
        if (count <= 0) {
            return "";
        }
        return String.join(",", Collections.nCopies(count, "?"));
    }
}
