package uk.nhs.adaptors.gp2gp.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void shouldRedactPemBlocks() {
        var value = "payload -----BEGIN PRIVATE KEY-----abc123-----END PRIVATE KEY----- payload";

        assertThat(LogSanitizer.sanitize(value))
            .contains("[REDACTED_PEM]")
            .doesNotContain("BEGIN PRIVATE KEY")
            .doesNotContain("abc123");
    }

    @Test
    void shouldTruncateLongValues() {
        var value = "a".repeat(2000);

        assertThat(LogSanitizer.sanitize(value))
            .contains("truncated")
            .hasSizeLessThan(1700);
    }
}
