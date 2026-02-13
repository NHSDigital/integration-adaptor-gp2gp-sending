package uk.nhs.adaptors.gp2gp.common.validation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import uk.nhs.adaptors.gp2gp.common.configuration.AppInitializer;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class StorageConnectorConfigurationValidationTest {

    // Valid configurations
    private static final String VALID_GP2GP_STORAGE_TYPE = "some-type";
    private static final String VALID_GP2GP_STORAGE_CONTAINER_NAME = "some-container-name";
    private static final String VALID_GP2GP_AZURE_STORAGE_CONNECTION_STRING = "some-connection-string";

    // Configuration fields
    private static final String GP2GP_STORAGE_TYPE = "type";
    private static final String GP2GP_STORAGE_CONTAINER_NAME = "containerName";
    private static final String GP2GP_AZURE_STORAGE_CONNECTION_STRING = "azureConnectionString";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestStorageConnectorClientConfiguration.class)
            .withBean(
                    "appInitializer",
                    AppInitializer.class,
                    () -> Mockito.mock(AppInitializer.class)
            );

    @Test
    void When_ConfigurationContainsAllProperties_Expect_IsContextIsCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_STORAGE_TYPE, VALID_GP2GP_STORAGE_TYPE),
                        buildPropertyValue(GP2GP_STORAGE_CONTAINER_NAME, VALID_GP2GP_STORAGE_CONTAINER_NAME),
                        buildPropertyValue(GP2GP_AZURE_STORAGE_CONNECTION_STRING, VALID_GP2GP_AZURE_STORAGE_CONNECTION_STRING)
                )
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(StorageConnectorConfiguration.class);

                    var storageConnectorConfiguration = context.getBean(StorageConnectorConfiguration.class);

                    assertAll(
                            () -> assertThat(storageConnectorConfiguration.getType()).isEqualTo(VALID_GP2GP_STORAGE_TYPE),
                            () -> assertThat(storageConnectorConfiguration.getContainerName()).isEqualTo(VALID_GP2GP_STORAGE_CONTAINER_NAME),
                            () -> assertThat(storageConnectorConfiguration.getAzureConnectionString()).isEqualTo(VALID_GP2GP_AZURE_STORAGE_CONNECTION_STRING)
                    );
                });
    }

    @Test
    void When_ConfigurationPropertiesNotProvided_Expect_ContextNotCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_STORAGE_TYPE, ""),
                        buildPropertyValue(GP2GP_STORAGE_CONTAINER_NAME, ""),
                        buildPropertyValue(GP2GP_AZURE_STORAGE_CONNECTION_STRING, "")
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    var startupFailure = context.getStartupFailure();

                    assertThat(startupFailure)
                            .rootCause()
                            .hasMessageContaining("Env variable not provided: GP2GP_STORAGE_TYPE")
                            .hasMessageContaining("Env variable not provided: GP2GP_STORAGE_CONTAINER_NAME");
//                            .hasMessageContaining("Env variable not provided: GP2GP_AZURE_STORAGE_CONNECTION_STRING");
                });
    }

    @Test
    void When_StorageTypeIsNotAzureAndConnectionStringIsEmpty_Expect_ContextCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_STORAGE_TYPE, VALID_GP2GP_STORAGE_TYPE),
                        buildPropertyValue(GP2GP_STORAGE_CONTAINER_NAME, VALID_GP2GP_STORAGE_CONTAINER_NAME),
                        buildPropertyValue(GP2GP_AZURE_STORAGE_CONNECTION_STRING, "")
                )
                .run(context -> {
                    var storageConnectorConfiguration = context.getBean(StorageConnectorConfiguration.class);

                    assertAll(
                            () -> assertThat(storageConnectorConfiguration.getType()).isEqualTo(VALID_GP2GP_STORAGE_TYPE),
                            () -> assertThat(storageConnectorConfiguration.getContainerName()).isEqualTo(VALID_GP2GP_STORAGE_CONTAINER_NAME),
                            () -> assertThat(storageConnectorConfiguration.getAzureConnectionString()).isEqualTo("")
                    );
                });
    }

    @Test
    void When_StorageTypeIsAzureAndConnectionStringIsEmpty_Expect_IsNotContextCreated() {
        contextRunner
                .withPropertyValues(
                        buildPropertyValue(GP2GP_STORAGE_TYPE, "Azure"),
                        buildPropertyValue(GP2GP_STORAGE_CONTAINER_NAME, VALID_GP2GP_STORAGE_CONTAINER_NAME),
                        buildPropertyValue(GP2GP_AZURE_STORAGE_CONNECTION_STRING, "")
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    var startupFailure = context.getStartupFailure();

                    assertThat(startupFailure)
                            .rootCause()
//                            .hasMessageContaining("Env variable not provided: GP2GP_STORAGE_TYPE")
//                            .hasMessageContaining("Env variable not provided: GP2GP_STORAGE_CONTAINER_NAME");
                            .hasMessageContaining("Env variable not provided: GP2GP_AZURE_STORAGE_CONNECTION_STRING");
                });
    }

    @Contract(pure = true)
    private static @NotNull String buildPropertyValue(String propertyName, String value) {
        return String.format("gp2gp.storage.%s=%s", propertyName, value);
    }

    @Configuration
    @EnableConfigurationProperties(StorageConnectorConfiguration.class)
    static class TestStorageConnectorClientConfiguration {
    }
}


