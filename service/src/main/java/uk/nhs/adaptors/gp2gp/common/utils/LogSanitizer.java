package uk.nhs.adaptors.gp2gp.common.utils;

import java.util.regex.Pattern;

public final class LogSanitizer {
    private static final int MAX_LOG_LENGTH = 1500;
    private static final Pattern PEM_BLOCK_PATTERN = Pattern.compile("-----BEGIN [^-]+-----.*?-----END [^-]+-----", Pattern.DOTALL);
    private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile(
        "(?i)((?:authorization|password|secret|token|clientkey|clientcert|rootca|subca)\\s*[:=]\\s*)(\"[^\"]*\"|[^\\s,;>]+)"
    );

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = PEM_BLOCK_PATTERN.matcher(value).replaceAll("[REDACTED_PEM]");
        sanitized = SENSITIVE_KEY_VALUE_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]");

        if (sanitized.length() <= MAX_LOG_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(0, MAX_LOG_LENGTH) + "... [truncated " + sanitized.length() + " chars]";
    }

    public static String summarizeLocation(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String normalized = value.replace('\\', '/');
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
