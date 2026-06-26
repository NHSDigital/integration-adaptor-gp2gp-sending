package uk.nhs.adaptors.gp2gp.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    public static final int COUNT_2000 = 2000;
    public static final int EXPECTED_1700 = 1700;

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
        var value = "a".repeat(COUNT_2000);

        assertThat(LogSanitizer.sanitize(value))
            .contains("truncated")
            .hasSizeLessThan(EXPECTED_1700);
    }
}
