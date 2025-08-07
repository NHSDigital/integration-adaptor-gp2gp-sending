package uk.nhs.adaptors.gp2gp.common.storage;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Testcontainers
class S3StorageConnectorTest {

    private static final String BUCKET_NAME = "s3bucket";
    private static final String FILE_NAME = "test-file.txt";

    private static S3StorageConnector s3StorageConnector;

    @Container
    private static final S3MockContainer S3_MOCK = new S3MockContainer("4.7.0");

    private static S3Client s3Client;

    @BeforeAll
    static void setUp() {
        s3Client = S3Client.builder()
            .endpointOverride(URI.create(S3_MOCK.getHttpEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("accessKey", "secretKey")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .region(Region.EU_WEST_2)
            .build();

        StorageConnectorConfiguration config = new StorageConnectorConfiguration();
        config.setContainerName(BUCKET_NAME);

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        s3StorageConnector = new S3StorageConnector(s3Client, config);
    }

    @Test
    void expectExceptionWhenFileDoesNotExist() {
        Exception exception = assertThrows(StorageConnectorException.class,
                                           () -> s3StorageConnector.downloadFromStorage("nonexistent-file.txt"));

        assertEquals("Error occurred downloading from S3 Bucket", exception.getMessage());
    }

    @Test
    void downloadFromStorageTest() throws IOException {

        String fileContent = "dummy-content";
        s3Client.putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(FILE_NAME).build(),
            RequestBody.fromString(fileContent));

        ResponseInputStream<GetObjectResponse> response = s3StorageConnector.downloadFromStorage(FILE_NAME);
        String downloadedContent = new String(response.readAllBytes(), StandardCharsets.UTF_8);

        assertNotNull(response);
        assertEquals(fileContent, downloadedContent);
    }

    @Test
    void uploadToStorageTest() throws IOException {
        String uploadContent = "upload-content";
        InputStream inputStream = new ByteArrayInputStream(uploadContent.getBytes(StandardCharsets.UTF_8));
        long streamLength = inputStream.available();

        s3StorageConnector.uploadToStorage(inputStream, streamLength, FILE_NAME);

        final var request = GetObjectRequest.builder().bucket(BUCKET_NAME).key(FILE_NAME).build();
        ResponseInputStream<GetObjectResponse> uploadedObjectInS3 = s3Client.getObject(request);
        String uploadedS3Content = new String(uploadedObjectInS3.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(uploadContent, uploadedS3Content);
    }

}