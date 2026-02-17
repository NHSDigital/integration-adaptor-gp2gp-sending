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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(classes = MongoClientConfigurationValidator.class)
public class MongoClientConfigurationValidationTest {

    // Valid configurations
    private static final String DEFAULT_GP2GP_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_GP2GP_MONGO_DATABASE = "gp2gp";
    private static final String VALID_GP2GP_MONGO_HOST = "host";
    private static final String VALID_GP2GP_MONGO_PORT = "1234";
    private static final String VALID_GP2GP_MONGO_USERNAME = "some-username";
    private static final String VALID_GP2GP_MONGO_PASSWORD = "some-password";
    private static final String VALID_GP2GP_MONGO_OPTIONS = "ssl=true;tls=true";
    private static final String EMPTY_CONFIGURATION_STRING = "";

    // Configuration fields
    private static final String GP2GP_MONGO_URI = "uri";
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
            );

    @Test
    void When_ConfigurationContainsOnlyUriProperty_Expect_IsContextIsCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_MONGO_URI, DEFAULT_GP2GP_MONGO_URI),
                        buildPropertyValue(GP2GP_MONGO_DATABASE, DEFAULT_GP2GP_MONGO_DATABASE),
                        buildPropertyValue(GP2GP_MONGO_HOST, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_PORT, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_USERNAME, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_PASSWORD, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_OPTIONS, EMPTY_CONFIGURATION_STRING)
                )
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(MongoClientConfiguration.class);

                    var mongoClientConfiguration = context.getBean(MongoClientConfiguration.class);

                    assertAll(
                            () -> assertThat(mongoClientConfiguration.getUri())
                                    .isEqualTo(DEFAULT_GP2GP_MONGO_URI),
                            () -> assertThat(mongoClientConfiguration.getDatabase())
                                    .isEqualTo(DEFAULT_GP2GP_MONGO_DATABASE),
                            () -> assertThat(mongoClientConfiguration.getHost())
                                    .isEqualTo(EMPTY_CONFIGURATION_STRING),
                            () -> assertThat(mongoClientConfiguration.getPort())
                                    .isEqualTo(EMPTY_CONFIGURATION_STRING),
                            () -> assertThat(mongoClientConfiguration.getUsername())
                                    .isEqualTo(EMPTY_CONFIGURATION_STRING),
                            () -> assertThat(mongoClientConfiguration.getPassword())
                                    .isEqualTo(EMPTY_CONFIGURATION_STRING),
                            () -> assertThat(mongoClientConfiguration.getOptions())
                                    .isEqualTo(EMPTY_CONFIGURATION_STRING)
                    );
                });
    }

    @Test
    void When_ConfigurationContainsAllPropertiesExceptUri_Expect_IsContextIsCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_MONGO_URI, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_DATABASE, DEFAULT_GP2GP_MONGO_DATABASE),
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
                            () -> assertThat(mongoClientConfiguration.getUri())
                                    .isEqualTo(EMPTY_CONFIGURATION_STRING),
                            () -> assertThat(mongoClientConfiguration.getDatabase())
                                    .isEqualTo(DEFAULT_GP2GP_MONGO_DATABASE),
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

    @Test
    void When_ConfigurationDoesNotContainAnyProperties_Expect_ContextIsNotCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_MONGO_URI, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_DATABASE, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_HOST, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_PORT, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_USERNAME, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_PASSWORD, EMPTY_CONFIGURATION_STRING),
                        buildPropertyValue(GP2GP_MONGO_OPTIONS, EMPTY_CONFIGURATION_STRING)
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    var startupFailure = context.getStartupFailure();

                    assertThat(startupFailure)
                            .rootCause()
                            .hasMessageContaining("Env variable not provided: GP2GP_MONGO_URI. Alternatively, provide the following env variables instead: " +
                                    "GP2GP_MONGO_DATABASE, GP2GP_MONGO_HOST, GP2GP_MONGO_OPTIONS, GP2GP_MONGO_PASSWORD, GP2GP_MONGO_PORT, GP2GP_MONGO_USERNAME");
                });
    }

    @Contract(pure = true)
    private static @NotNull String buildPropertyValue(String propertyName, String value) {
        return String.format("gp2gp.mongodb.%s=%s", propertyName, value);
    }

    @Configuration
    @EnableConfigurationProperties(MongoClientConfiguration.class)
    static class TestMongoClientConfiguration {
    }
}


