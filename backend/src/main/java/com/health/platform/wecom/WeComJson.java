package com.health.platform.wecom;

final class WeComJson {
    private WeComJson() {}

    static String stringValue(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        int colon = json.indexOf(':', start + pattern.length());
        if (colon < 0) return null;
        int quote = json.indexOf('"', colon + 1);
        if (quote < 0) return null;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return null;
    }

    static long longValue(String json, String key, long fallback) {
        if (json == null) return fallback;
        String pattern = "\"" + key + "\"";
        int start = json.indexOf(pattern);
        if (start < 0) return fallback;
        int colon = json.indexOf(':', start + pattern.length());
        if (colon < 0) return fallback;
        int end = colon + 1;
        while (end < json.length() && Character.isWhitespace(json.charAt(end))) end++;
        int valueStart = end;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Long.parseLong(json.substring(valueStart, end));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
