package uk.nhs.adaptors.gp2gp.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureStorageConnectorTest {
    private static final String FILENAME = "test-file.txt";

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    private AzureStorageConnector azureStorageConnector;

    @BeforeEach
    void setUp() throws Exception {
        azureStorageConnector = new AzureStorageConnector();

        Field field = AzureStorageConnector.class.getDeclaredField("containerClient");
        field.setAccessible(true);
        field.set(azureStorageConnector, containerClient);

        when(containerClient.getBlobClient(FILENAME)).thenReturn(blobClient);
    }

    @Test
    void When_UploadSucceeds_Expect_NoException() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test-content".getBytes());
        doNothing().when(blobClient).upload(inputStream, inputStream.available());

        azureStorageConnector.uploadToStorage(inputStream, inputStream.available(), FILENAME);

        verify(blobClient).upload(inputStreamCaptor.capture(), eq((long) inputStream.available()));
    }

    @Test
    void When_UploadFails_Expect_StorageConnectorException() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test-content".getBytes());
        doThrow(new RuntimeException("Upload failed")).when(blobClient).upload(inputStream, inputStream.available());

        var exception = assertThrows(StorageConnectorException.class,
                () -> azureStorageConnector.uploadToStorage(inputStream, inputStream.available(), FILENAME));

        assertEquals("Error occurred uploading to Azure Storage", exception.getMessage());
    }

    @Test
    void When_DownloadFails_Expect_StorageConnectorException() {

        when(blobClient.openInputStream()).thenThrow(new RuntimeException("Download failed"));

        var exception = assertThrows(StorageConnectorException.class,
                () -> azureStorageConnector.downloadFromStorage(FILENAME));

        assertEquals("Error occurred downloading from Azure Storage", exception.getMessage());

    }
}



