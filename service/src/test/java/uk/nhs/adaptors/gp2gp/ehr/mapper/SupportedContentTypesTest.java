package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportedContentTypesTest {

    private SupportedContentTypes supportedContentTypes;

    @BeforeEach
    void setUp() {
        supportedContentTypes = new SupportedContentTypes();
    }

    @Test
    void When_CheckingIfExecutableContentTypeIsSupported_Expect_False() {
        assertThat(supportedContentTypes.isContentTypeSupported("application/x-dosexec")).isFalse();
    }

    @Test
    void When_CheckingIfNotSupportedContentTypeIsSupported_Expect_False() {
        assertThat(supportedContentTypes.isContentTypeSupported("application/octet-stream")).isFalse();
    }

    @Test
    void When_CheckingIfSupportedContentTypeIsSupported_Expect_True() {
        assertThat(supportedContentTypes.isContentTypeSupported("text/plain")).isTrue();
    }
}
