package uk.nhs.adaptors.gp2gp.common.utils;

import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;

public final class LogSanitizer {

    private static int maxLogLength;
    private static final Pattern PEM_BLOCK_PATTERN = Pattern.compile("-----BEGIN [^-]+-----.*?-----END [^-]+-----", Pattern.DOTALL);
    private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile(
        "(?i)((?:authorization|password|secret|token|clientkey|clientcert|rootca|subca)\\s*[:=]\\s*)(\"[^\"]*\"|[^\\s,;>]+)"
    );

    private LogSanitizer() {
    }

    @Value("${logging.sanitizer.max-message-length}")
    public static void setMaxLogLength(int maxLength) {
        maxLogLength = maxLength;
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = PEM_BLOCK_PATTERN.matcher(value).replaceAll("[REDACTED_PEM]");
        sanitized = SENSITIVE_KEY_VALUE_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]");

        if (sanitized.length() <= maxLogLength) {
            return sanitized;
        }

        return sanitized.substring(0, maxLogLength) + "... [truncated " + sanitized.length() + " chars]";
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

        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
