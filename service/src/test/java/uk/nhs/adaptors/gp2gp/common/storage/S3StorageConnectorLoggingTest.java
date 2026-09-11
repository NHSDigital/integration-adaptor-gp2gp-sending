package uk.nhs.adaptors.gp2gp.common.storage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageConnectorLoggingTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String FILE_NAME = "test-file.txt";
    private static final long STREAM_LENGTH = 5L;

    @Mock
    private S3Client s3Client;

    private S3StorageConnector s3StorageConnector;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        StorageConnectorConfiguration config = new StorageConnectorConfiguration();
        config.setContainerName(BUCKET_NAME);

        logger = (Logger) LoggerFactory.getLogger(S3StorageConnector.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        s3StorageConnector = new S3StorageConnector(s3Client, config);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    void When_ConstructingConnector_Expect_DebugLog() {
        assertContains(Level.DEBUG, "S3StorageConnector initialized with bucket: " + BUCKET_NAME);
    }

    @Test
    void When_UploadSucceeds_Expect_InfoLogs() {
        InputStream inputStream = new ByteArrayInputStream("hello".getBytes());
        long streamLength = STREAM_LENGTH;

        s3StorageConnector.uploadToStorage(inputStream, streamLength, FILE_NAME);

        assertContains(Level.INFO, "Uploading to S3 bucket: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME + ", size=5");
        assertContains(Level.INFO, "Successfully uploaded to S3: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
    }

    @Test
    void When_UploadFails_Expect_ErrorLogAndException() {
        InputStream inputStream = new ByteArrayInputStream("hello".getBytes());
        long streamLength = STREAM_LENGTH;
        doThrow(new RuntimeException("upload failed"))
            .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        StorageConnectorException exception = assertThrows(StorageConnectorException.class,
            () -> s3StorageConnector.uploadToStorage(inputStream, streamLength, FILE_NAME));

        assertEquals("Error occurred uploading to S3 Bucket", exception.getMessage());
        assertContains(Level.ERROR,
                "Error occurred uploading to S3 Bucket: bucket=" + BUCKET_NAME
                        + ", filename=" + FILE_NAME + ", size=5");
    }

    @Test
    void When_DownloadSucceeds_Expect_InfoLogs() {

        ResponseInputStream<GetObjectResponse> response
                = (ResponseInputStream<GetObjectResponse>) org.mockito.Mockito.mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        ResponseInputStream<GetObjectResponse> result = s3StorageConnector.downloadFromStorage(FILE_NAME);

        assertEquals(response, result);
        assertContains(Level.INFO,
                "Downloading from S3 bucket: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
        assertContains(Level.INFO,
                "Successfully downloaded from S3: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
    }

    @Test
    void When_DownloadFails_Expect_ErrorLogAndException() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("download failed"));

        StorageConnectorException exception = assertThrows(StorageConnectorException.class,
            () -> s3StorageConnector.downloadFromStorage(FILE_NAME));

        assertEquals("Error occurred downloading from S3 Bucket", exception.getMessage());
        assertContains(Level.ERROR, "Error occurred downloading from S3 Bucket: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
    }

    private void assertContains(Level level, String expectedMessagePart) {
        List<String> messages = logAppender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertThat(messages).anyMatch(message -> message.contains(expectedMessagePart));
    }
}
