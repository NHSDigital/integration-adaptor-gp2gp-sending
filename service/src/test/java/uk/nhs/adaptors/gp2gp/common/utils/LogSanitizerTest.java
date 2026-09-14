package uk.nhs.adaptors.gp2gp.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSanitizerTest {

    public static final int COUNT_2000 = 2000;
    public static final int COUNT_2001 = 2001;
    public static final int COUNT_3000 = 3000;
    public static final int MAX_LENGTH_2000 = 2000;

    @BeforeEach
    void setUp() {
        LogSanitizer.setMaxLogLength(MAX_LENGTH_2000);
    }

    @Test
    void shouldRedactPemBlocks() {
        var value = "payload -----BEGIN PRIVATE KEY-----abc123-----END PRIVATE KEY----- payload";

        assertThat(LogSanitizer.sanitize(value))
            .contains("[REDACTED_PEM]")
            .doesNotContain("BEGIN PRIVATE KEY")
            .doesNotContain("abc123");
    }

    @Test
    void shouldReturnNullForNullSanitizeInput() {
        assertNull(LogSanitizer.sanitize(null));
    }

    @Test
    void shouldTruncateLongValues() {
        var value = "b".repeat(COUNT_3000);

        int charLimit = COUNT_2000 + "... [truncated 2000 chars]".length() + 1;

        assertThat(LogSanitizer.sanitize(value))
            .contains("truncated")
            .hasSizeLessThan(charLimit);
    }

    @Test
    void shouldNotTruncateValuesAtBoundaryLength() {
        var value = "a".repeat(COUNT_2000);

        assertEquals(value, LogSanitizer.sanitize(value));
    }

    @Test
    void shouldTruncateValuesAboveBoundaryLength() {
        var value = "a".repeat(COUNT_2001);

        assertThat(LogSanitizer.sanitize(value))
            .startsWith("a".repeat(COUNT_2000))
            .contains("truncated");
    }

    @Test
    void shouldSummarizeWindowsStyleLocationToFileName() {
        assertThat(LogSanitizer.summarizeLocation("s3://bucket/path/to/truststore.p12"))
            .isEqualTo("truststore.p12");

        assertThat(LogSanitizer.summarizeLocation("/truststore.p12"))
            .isEqualTo("truststore.p12");

        assertThat(LogSanitizer.summarizeLocation("?version=1"))
            .isEqualTo("");

        assertThat(LogSanitizer.summarizeLocation("C:\\config\\certs\\client-key.pem"))
            .isEqualTo("client-key.pem")
            .doesNotContain("\\");

        assertThat(LogSanitizer.summarizeLocation("C:\\config\\certs\\client-key.pem?version=1"))
            .isEqualTo("client-key.pem");
    }

    @Test
    void shouldSummarizeWindowsStyleLocationToFileName2() {
        assertThat(LogSanitizer.summarizeLocation("s3://bucket/path/to/truststore.p12"))
                .isEqualTo("truststore.p12");
    }

    @Test
    void shouldReturnOriginalValueForBlankOrSimpleLocation() {
        assertEquals("", LogSanitizer.summarizeLocation(""));
        assertEquals(" ", LogSanitizer.summarizeLocation(" "));

        assertNull(LogSanitizer.summarizeLocation(null));

        assertEquals("truststore.p12", LogSanitizer.summarizeLocation("truststore.p12"));
    }

}
