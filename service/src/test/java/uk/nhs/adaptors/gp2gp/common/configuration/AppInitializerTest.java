package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;
import javax.naming.ConfigurationException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppInitializerTest {

    public static final String EXPECTED_ERROR_MESSAGE = "S3Client cannot be instantiated: "
                                                        + "Trust store URL is either not set or does not start with the 's3://' prefix.";
    private AppInitializer appInitializer;
    private StorageConnectorConfiguration storageConnectorConfiguration;

    @BeforeEach
    void setUp() {
        storageConnectorConfiguration = new StorageConnectorConfiguration();
    }

    @Test
    void getNullWhenTrustStoreUrlDoesNotExists() {

        storageConnectorConfiguration.setTrustStoreUrl(null);
        appInitializer = new AppInitializer(storageConnectorConfiguration);

        Exception exception = assertThrows(ConfigurationException.class, () -> appInitializer.getS3Client());

        assertEquals(EXPECTED_ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void getNullWhenTrustStoreUrlDoesNotStartWithS3Prefix() {

        storageConnectorConfiguration.setTrustStoreUrl("http://localhost");
        appInitializer = new AppInitializer(storageConnectorConfiguration);

        Exception exception = assertThrows(ConfigurationException.class, () -> appInitializer.getS3Client());

        assertEquals(EXPECTED_ERROR_MESSAGE, exception.getMessage());
    }
}