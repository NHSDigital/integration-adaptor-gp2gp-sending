package uk.nhs.adaptors.gp2gp.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageConnectorTest {

    private static final String FILE_NAME = "test-file.txt";
    private S3StorageConnector s3StorageConnector;
    private StorageConnectorConfiguration config;
    private static final long STREAM_LENGTH = 100L;

    @Mock
    private S3Client mockS3Client;

    @Mock
    private InputStream is;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        config = new StorageConnectorConfiguration();
        config.setContainerName("s3Bucket");

        s3StorageConnector = new S3StorageConnector(mockS3Client, config);
    }


    @Test
    void expectExceptionWhenS3ClientCannotDeliverResponse() {
        S3StorageConnector storageConnector = new S3StorageConnector(S3Client.builder().region(Region.EU_WEST_2).build(), config);
        Exception exception = assertThrows(StorageConnectorException.class,
                                           () -> storageConnector.downloadFromStorage("s3File"));

        assertEquals("Error occurred downloading from S3 Bucket", exception.getMessage());
    }

    @Test
    void downloadFromStorageTest() {
        var mockResponse = mock(GetObjectResponse.class);
        var mockInputStream = new ByteArrayInputStream("dummy-content".getBytes());
        var mockResponseInputStream = new ResponseInputStream<>(mockResponse, mockInputStream);
        final var request = GetObjectRequest.builder().bucket(config.getContainerName()).key(FILE_NAME).build();

        when(mockS3Client.getObject(request)).thenReturn(mockResponseInputStream);

        var result = s3StorageConnector.downloadFromStorage(FILE_NAME);

        assertNotNull(result);
        verify(mockS3Client).getObject(request);
    }

    @Test
    void uploadToStorageTest() {
        final var expectedRequest = PutObjectRequest.builder().bucket(config.getContainerName()).key(FILE_NAME).build();

        s3StorageConnector.uploadToStorage(is, STREAM_LENGTH, FILE_NAME);

        verify(mockS3Client, times(1)).putObject(eq(expectedRequest), any(RequestBody.class));
    }

}