package uk.nhs.adaptors.gp2gp.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageConnectorTest {

    private static final String FILE_NAME = "test-file.txt";
    private S3StorageConnector s3StorageConnector;
    private StorageConnectorConfiguration config;
    @Mock
    private S3Client mockS3Client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        config = new StorageConnectorConfiguration();
        config.setContainerName("s3Bucket");

        s3StorageConnector = new S3StorageConnector(mockS3Client, config);
    }


    @Test
    void expectExceptionWhenS3ClientCantDeliverResponse() {
        S3StorageConnector storageConnector = new S3StorageConnector(S3Client.builder().build(), config);
        Exception exception = assertThrows(StorageConnectorException.class,
                                           () -> storageConnector.downloadFromStorage("s3File"));

        assertEquals("Error occurred downloading from S3 Bucket", exception.getMessage());
    }

    @Test
    void downloadFromStorage() {
        var mockResponse = mock(GetObjectResponse.class);
        var mockInputStream = new ByteArrayInputStream("dummy-content".getBytes());
        var mockResponseInputStream = new ResponseInputStream<>(mockResponse, mockInputStream);
        final var request = GetObjectRequest.builder().bucket(config.getContainerName()).key(FILE_NAME).build();

        when(mockS3Client.getObject(request)).thenReturn(mockResponseInputStream);

        var result = s3StorageConnector.downloadFromStorage(FILE_NAME);

        assertNotNull(result);
        verify(mockS3Client).getObject(request);
    }
}