package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppInitializerTest {

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

        S3Client s3Client = appInitializer.getS3Client();

        assertNull(s3Client);
    }

    @Test
    void getNullWhenTrustStoreUrlDoesNotStartWithS3Prefix() {

        storageConnectorConfiguration.setTrustStoreUrl("http://localhost");
        appInitializer = new AppInitializer(storageConnectorConfiguration);

        S3Client s3Client = appInitializer.getS3Client();

        assertNull(s3Client);
    }
}