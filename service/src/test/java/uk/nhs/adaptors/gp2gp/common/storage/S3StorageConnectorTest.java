package uk.nhs.adaptors.gp2gp.common.storage;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class S3StorageConnectorTest {

    public static final int PORT = 9090;
    private static final String BUCKET_NAME = "s3bucket";
    private static final String FILE_NAME = "test-file.txt";

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().withSecureConnection(false).build();

    private static S3StorageConnector s3StorageConnector;
    private static StorageConnectorConfiguration config;

    private static S3Client s3Client;

    @BeforeAll
    static void setUp() {

        s3Client = S3_MOCK.createS3ClientV2();

        config = new StorageConnectorConfiguration();
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
        InputStream inputStream = new ByteArrayInputStream("upload-content".getBytes(StandardCharsets.UTF_8));
        long streamLength = inputStream.available();

        s3StorageConnector.uploadToStorage(inputStream, streamLength, FILE_NAME);

        HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key(FILE_NAME).build());

        assertNotNull(response);
        assertEquals(Optional.of(streamLength).get(), response.contentLength());
    }

}