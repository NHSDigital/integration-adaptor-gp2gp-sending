package uk.nhs.adaptors.gp2gp.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageConnectorConfigurationTest {

    private StorageConnectorConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new StorageConnectorConfiguration();
        configuration.setTrustStoreUrl("s3://my-trust-store");
    }

    @Test
    void expectToReturnS3ClientTest() {
        var s3Client = configuration.getS3Client();

        assertNotNull(s3Client);
    }
}