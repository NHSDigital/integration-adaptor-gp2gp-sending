package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

@Slf4j
public class AzureStorageConnector implements StorageConnector {
    @Autowired
    private BlobContainerClient containerClient;

    protected AzureStorageConnector() {
        LOGGER.debug("AzureStorageConnector initialized");
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        LOGGER.info("Uploading to Azure Storage: filename={}, size={}", filename, streamLength);
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            blobClient.upload(is, streamLength);
            LOGGER.info("Successfully uploaded to Azure Storage: filename={}", filename);
        } catch (Exception exception) {
            LOGGER.error("Error occurred during uploading to Azure Storage: filename={}, size={}", filename, streamLength, exception);
            throw new StorageConnectorException("Error occurred uploading to Azure Storage", exception);
        }
    }

    @Override
    public InputStream downloadFromStorage(String filename) throws StorageConnectorException {
        LOGGER.info("Downloading from Azure Storage: filename={}", filename);
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            InputStream result = blobClient.openInputStream();
            LOGGER.info("Successfully downloaded from Azure Storage: filename={}", filename);
            return result;
        } catch (Exception exception) {
            LOGGER.error("Error occurred downloading from Azure Storage: filename={}", filename, exception);
            throw new StorageConnectorException("Error occurred downloading from Azure Storage", exception);
        }
    }
}
