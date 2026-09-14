package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Slf4j
public class S3StorageConnector implements StorageConnector {

    private final S3Client s3client;
    private final String bucketName;

    protected S3StorageConnector(S3Client s3client, StorageConnectorConfiguration configuration) {
        this.bucketName = configuration.getContainerName();
        this.s3client = s3client;
        LOGGER.debug("S3StorageConnector initialized with bucket: {}", bucketName);
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        LOGGER.info("Uploading to S3 bucket: bucket={}, filename={}, size={}", bucketName, filename, streamLength);
        try {
            final var putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(filename).build();

            s3client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(is, streamLength)
            );
            LOGGER.info("Successfully uploaded to S3: bucket={}, filename={}", bucketName, filename);
        } catch (Exception exception) {
            LOGGER.error("Error occurred uploading to S3 Bucket: bucket={}, filename={}, size={}",
                bucketName, filename, streamLength, exception);
            throw new StorageConnectorException("Error occurred uploading to S3 Bucket", exception);
        }
    }

    @Override
    public ResponseInputStream<GetObjectResponse> downloadFromStorage(String filename) throws StorageConnectorException {
        LOGGER.info("Downloading from S3 bucket: bucket={}, filename={}", bucketName, filename);
        try {
            final var request = GetObjectRequest.builder().bucket(bucketName).key(filename).build();
            ResponseInputStream<GetObjectResponse> response = s3client.getObject(request);
            LOGGER.info("Successfully downloaded from S3: bucket={}, filename={}", bucketName, filename);
            return response;
        } catch (Exception exception) {
            LOGGER.error("Error occurred downloading from S3 Bucket: bucket={}, filename={}", bucketName, filename, exception);
            throw new StorageConnectorException("Error occurred downloading from S3 Bucket", exception);
        }
    }
}
