package uk.nhs.adaptors.gp2gp.common.validation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class GpcConfigurationValidationTest {

    // Valid configurations
    private static final int VALID_LARGE_ATTACHMENT_THRESHOLD = "4500000";
    private static final int VALID_LARGE_EHR_EXTRACT_THRESHOLD = "4500000";

    // Configuration fields
    private static final String LARGE_ATTACHMENT_THRESHOLD = "largeAttachmentThreshold";
    private static final String LARGE_EHR_EXTRACT_THRESHOLD = "largeEhrExtractThreshold";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestGp2gpConfiguration.class);

    @Test
    void When_GpcConfigurationContainsAllProperties_Expect_IsContextIsCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(LARGE_ATTACHMENT_THRESHOLD, VALID_LARGE_ATTACHMENT_THRESHOLD),
                        buildPropertyValue(LARGE_EHR_EXTRACT_THRESHOLD, VALID_LARGE_EHR_EXTRACT_THRESHOLD),
                )
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(GpcConfiguration.class);

                    var gpcConfiguration = context.getBean(GpcConfiguration.class);

                    assertAll(
                            () -> assertThat(gpcConfiguration.getLargeAttachmentThreshold()).isEqualTo(LARGE_ATTACHMENT_THRESHOLD)
//                            () -> assertThat(gpcConfiguration.getClientKey()).isEqualTo(VALID_RSA_PRIVATE_KEY),
//                            () -> assertThat(gpcConfiguration.getRootCA()).isEqualTo(VALID_CERTIFICATE),
//                            () -> assertThat(gpcConfiguration.getSubCA()).isEqualTo(VALID_CERTIFICATE),
//                            () -> assertThat(gpcConfiguration.isSslEnabled()).isTrue()
                    );
                });
    }

//    @Test
//    void When_GpcConfigurationNoSslProperties_Expect_IsContextIsCreatedAndShouldUseSslIsFalse() {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue(CLIENT_CERT, ""),
//                        buildPropertyValue(CLIENT_KEY, ""),
//                        buildPropertyValue(ROOT_CA, ""),
//                        buildPropertyValue(SUB_CA, "")
//                )
//                .run(context -> {
//                    assertThat(context)
//                            .hasNotFailed()
//                            .hasSingleBean(GpcConfiguration.class);
//
//                    var gpcConfiguration = context.getBean(GpcConfiguration.class);
//
//                    assertAll(
//                            () -> assertThat(gpcConfiguration.getClientCert()).isEmpty(),
//                            () -> assertThat(gpcConfiguration.getClientKey()).isEmpty(),
//                            () -> assertThat(gpcConfiguration.getRootCA()).isEmpty(),
//                            () -> assertThat(gpcConfiguration.getSubCA()).isEmpty(),
//                            () -> assertThat(gpcConfiguration.isSslEnabled()).isFalse()
//                    );
//                });
//    }
//
//    @Test
//    void When_GpcConfigurationHasSomeButNotAllSslProperties_Expect_ContextNotCreated(
//    ) {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue(CLIENT_CERT, ""),
//                        buildPropertyValue(CLIENT_KEY, VALID_RSA_PRIVATE_KEY),
//                        buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
//                        buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
//                )
//                .run(context -> {
//                    assertThat(context).hasFailed();
//                    var startupFailure = context.getStartupFailure();
//
//                    assertThat(startupFailure)
//                            .rootCause()
//                            .hasMessageContaining("To enable mutual TLS you must provide GPC_CONSUMER_SPINE_CLIENT_CERT environment variable(s).")
//                            .hasMessageContaining(
//                                    "To disable mutual TLS you must remove GPC_CONSUMER_SPINE_CLIENT_KEY, GPC_CONSUMER_SPINE_ROOT_CA_CERT, "
//                                            + "GPC_CONSUMER_SPINE_SUB_CA_CERT environment variable(s).");
//                });
//    }
//
//    @Test
//    void When_GpcConfigurationHasAnInvalidCertificate_Expect_ContextIsNotCreated() {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue(CLIENT_CERT, INVALID_CERTIFICATE),
//                        buildPropertyValue(CLIENT_KEY, VALID_RSA_PRIVATE_KEY),
//                        buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
//                        buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
//                )
//                .run(context -> {
//                    assertThat(context).hasFailed();
//
//                    var startupFailure = context.getStartupFailure();
//
//                    assertThat(startupFailure)
//                            .rootCause()
//                            .hasMessageContaining(
//                                    "The environment variable(s) GPC_CONSUMER_SPINE_CLIENT_CERT are not in a valid PEM format"
//                            );
//                });
//    }
//
//    @Test
//    void When_GpcConfigurationHasAnInvalidClientKey_Expect_ContextIsNotCreated() {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue(CLIENT_CERT, VALID_CERTIFICATE),
//                        buildPropertyValue(CLIENT_KEY, INVALID_RSA_PRIVATE_KEY),
//                        buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
//                        buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
//                )
//                .run(context -> {
//                    assertThat(context).hasFailed();
//
//                    var startupFailure = context.getStartupFailure();
//
//                    assertThat(startupFailure)
//                            .rootCause()
//                            .hasMessageContaining(
//                                    "The environment variable(s) GPC_CONSUMER_SPINE_CLIENT_KEY are not in a valid PEM format"
//                            );
//                });
//    }
//
//    @Test
//    void When_GpcConfigurationHasSspUrlPresentWithTrailingSlash_Expect_ContextIsCreatedAndIsSspEnabled() {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue("sspUrl", "/this-is-a-url.com/")
//                )
//                .run(context -> {
//                    assertThat(context)
//                            .hasNotFailed()
//                            .hasSingleBean(GpcConfiguration.class);
//
//                    var gpcConfiguration = context.getBean(GpcConfiguration.class);
//                    assertThat(gpcConfiguration.isSspEnabled()).isTrue();
//                });
//    }
//
//    @Test
//    void When_GpcConfigurationHasSspUrlPresentWithoutTrailingSlash_Expect_ContextIsCreatedAndIsSspEnabledAndUrlHasTrailingSlash() {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue("sspUrl", "/this-is-a-url.com")
//                )
//                .run(context -> {
//                    assertThat(context)
//                            .hasNotFailed()
//                            .hasSingleBean(GpcConfiguration.class);
//
//                    var gpcConfiguration = context.getBean(GpcConfiguration.class);
//
//                    assertAll(
//                            () -> assertThat(gpcConfiguration.isSspEnabled()).isTrue(),
//                            () -> assertThat(gpcConfiguration.getSspUrl()).isEqualTo("/this-is-a-url.com/")
//                    );
//                });
//    }
//
//    @Test
//    void When_GpcConfigurationDoesNotHaveSspUrlPresent_Expect_ContextIsCreatedAndSspIsNotEnabled() {
//        contextRunner
//                .withPropertyValues(
//                        buildPropertyValue("sspUrl", "")
//                )
//                .run(context -> {
//                    assertThat(context)
//                            .hasNotFailed()
//                            .hasSingleBean(GpcConfiguration.class);
//
//                    var gpcConfiguration = context.getBean(GpcConfiguration.class);
//                    assertThat(gpcConfiguration.isSspEnabled()).isFalse();
//                });
//    }

    @Contract(pure = true)
    private static @NotNull String buildPropertyValue(String propertyName, String value) {
        return String.format("gpc-consumer.gpc.%s=%s", propertyName, value);
    }

    @Configuration
    @EnableConfigurationProperties(Gp2gpConfiguration.class)
    static class TestGp2gpConfiguration {
    }
}


