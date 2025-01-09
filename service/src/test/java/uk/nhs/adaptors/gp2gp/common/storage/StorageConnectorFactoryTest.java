package uk.nhs.adaptors.gp2gp.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StorageConnectorFactoryTest {

    private StorageConnectorFactory storageConnectorFactory;

    @BeforeEach
    void setUp() {
        storageConnectorFactory = new StorageConnectorFactory();
        StorageConnectorConfiguration configuration = new StorageConnectorConfiguration();
        configuration.setType(StorageConnectorOptions.S3.getStringValue());
        storageConnectorFactory.setConfiguration(configuration);
    }

    @Test
    void storageConnectorFactoryReturnsS3StorageConnectorTest() {
        StorageConnector storageConnector = storageConnectorFactory.getObject();

        assertInstanceOf(S3StorageConnector.class, storageConnector);
    }
}