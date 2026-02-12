package uk.nhs.adaptors.gp2gp.common.validation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import uk.nhs.adaptors.gp2gp.common.configuration.AppInitializer;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(classes = MongoClientConfigurationValidator.class)
public class MongoClientConfigurationValidationTest {

    // Valid configurations
    private static final String VALID_GP2GP_MONGO_DATABASE = "test-db";
    private static final String VALID_GP2GP_MONGO_HOST = "host";
    private static final String VALID_GP2GP_MONGO_PORT = "1234";
    private static final String VALID_GP2GP_MONGO_USERNAME = "some-username";
    private static final String VALID_GP2GP_MONGO_PASSWORD = "some-password";
    private static final String VALID_GP2GP_MONGO_OPTIONS = "ssl=true;tls=true";

    // Configuration fields
    private static final String GP2GP_MONGO_DATABASE = "database";
    private static final String GP2GP_MONGO_HOST = "host";
    private static final String GP2GP_MONGO_PORT = "port";
    private static final String GP2GP_MONGO_USERNAME = "username";
    private static final String GP2GP_MONGO_PASSWORD = "password";
    private static final String GP2GP_MONGO_OPTIONS = "options";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestMongoClientConfiguration.class)
            .withBean(
                    "appInitializer",
                    AppInitializer.class,
                    () -> Mockito.mock(AppInitializer.class)
            )
            .withBean(
                    "storageConnectorConfiguration",
                    StorageConnectorConfiguration.class,
                    () -> Mockito.mock(StorageConnectorConfiguration.class)
            );

    @Test
    void When_ConfigurationContainsAllProperties_Expect_IsContextIsCreated() {

        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_MONGO_DATABASE,  VALID_GP2GP_MONGO_DATABASE),
                        buildPropertyValue(GP2GP_MONGO_HOST, VALID_GP2GP_MONGO_HOST),
                        buildPropertyValue(GP2GP_MONGO_PORT, VALID_GP2GP_MONGO_PORT),
                        buildPropertyValue(GP2GP_MONGO_USERNAME, VALID_GP2GP_MONGO_USERNAME),
                        buildPropertyValue(GP2GP_MONGO_PASSWORD, VALID_GP2GP_MONGO_PASSWORD),
                        buildPropertyValue(GP2GP_MONGO_OPTIONS, VALID_GP2GP_MONGO_OPTIONS)
                )
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(MongoClientConfiguration.class);

                    var mongoClientConfiguration = context.getBean(MongoClientConfiguration.class);

                    assertAll(
                            () -> assertThat(mongoClientConfiguration.getDatabase())
                                    .isEqualTo(VALID_GP2GP_MONGO_DATABASE),
                            () -> assertThat(mongoClientConfiguration.getHost())
                                    .isEqualTo(VALID_GP2GP_MONGO_HOST),
                            () -> assertThat(mongoClientConfiguration.getPort())
                                    .isEqualTo(VALID_GP2GP_MONGO_PORT),
                            () -> assertThat(mongoClientConfiguration.getUsername())
                                    .isEqualTo(VALID_GP2GP_MONGO_USERNAME),
                            () -> assertThat(mongoClientConfiguration.getPassword())
                                    .isEqualTo(VALID_GP2GP_MONGO_PASSWORD),
                            () -> assertThat(mongoClientConfiguration.getOptions())
                                    .isEqualTo(VALID_GP2GP_MONGO_OPTIONS)
                    );
                });
    }

//    @Test
//    void When_ConfigurationPropertiesNotProvided_Expect_ContextNotCreated() {
//        contextRunner
//                .withPropertyValues(
////                        buildPropertyValue(LARGE_ATTACHMENT_THRESHOLD, ""),
//                        buildPropertyValue(GP2GP_MONGO_PORT, "")
//                )
//                .run(context -> {
//                    assertThat(context).hasFailed();
//                    var startupFailure = context.getStartupFailure();
//
//                    assertThat(startupFailure)
//                            .rootCause()
//                            .hasMessageContaining("LARGE_ATTACHMENT_THRESHOLD not provided")
//                            .hasMessageContaining("LARGE_EHR_EXTRACT_THRESHOLD not provided");
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
        return String.format("gp2gp.mongodb.%s=%s", propertyName, value);
    }

    @Configuration
    @EnableConfigurationProperties(MongoClientConfiguration.class)
    static class TestMongoClientConfiguration {
    }
}


