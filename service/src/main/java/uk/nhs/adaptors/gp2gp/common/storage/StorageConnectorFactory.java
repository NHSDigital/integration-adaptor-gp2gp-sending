package uk.nhs.adaptors.gp2gp.common.storage;

import org.springframework.beans.factory.FactoryBean;
import lombok.Setter;
import software.amazon.awssdk.services.s3.S3Client;

@Setter
public class StorageConnectorFactory implements FactoryBean<StorageConnector> {
    private StorageConnector storageConnector;

    private StorageConnectorConfiguration configuration;

    @Override
    public StorageConnector getObject() {
        if (storageConnector == null) {
            switch (StorageConnectorOptions.enumOf(configuration.getType())) {
                case S3:
                    storageConnector = new S3StorageConnector(S3Client.builder().build(), configuration);
                    break;
                case AZURE:
                    storageConnector = new AzureStorageConnector();
                    break;
                default:
                    storageConnector = new LocalMockConnector();
            }
        }
        return storageConnector;
    }

    @Override
    public Class<?> getObjectType() {
        return StorageConnector.class;
    }
}
