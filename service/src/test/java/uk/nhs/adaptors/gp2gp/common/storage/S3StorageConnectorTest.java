package uk.nhs.adaptors.gp2gp.common.storage;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.slf4j.LoggerFactory;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Testcontainers
class S3StorageConnectorTest {

    private static final String BUCKET_NAME = "s3bucket";
    private static final String FILE_NAME = "test-file.txt";

    private static S3StorageConnector s3StorageConnector;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;
    private Level originalLevel;

    @Container
    private static final S3MockContainer S3_MOCK = new S3MockContainer("4.7.0").withReuse(true);

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

    @BeforeEach
    void setUpLogCapture() {
        logger = (Logger) LoggerFactory.getLogger(S3StorageConnector.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDownLogCapture() {
        logger.detachAppender(logAppender);
        logAppender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    void expectExceptionWhenFileDoesNotExist() {
        Exception exception = assertThrows(StorageConnectorException.class,
                                           () -> s3StorageConnector.downloadFromStorage("nonexistent-file.txt"));

        assertEquals("Error occurred downloading from S3 Bucket", exception.getMessage());
        assertContains(Level.INFO,
                "Downloading from S3 bucket: bucket=" + BUCKET_NAME + ", filename=nonexistent-file.txt");
        assertContains(Level.ERROR,
                "Error occurred downloading from S3 Bucket: bucket=" + BUCKET_NAME + ", filename=nonexistent-file.txt");
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
        assertContains(Level.INFO,
                "Downloading from S3 bucket: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
        assertContains(Level.INFO,
                "Successfully downloaded from S3: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
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
        assertContains(Level.INFO,
                "Uploading to S3 bucket: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME + ", size=" + streamLength);
        assertContains(Level.INFO,
                "Successfully uploaded to S3: bucket=" + BUCKET_NAME + ", filename=" + FILE_NAME);
    }

    @Test
    void constructorShouldLogDebugMessage() {
        StorageConnectorConfiguration config = new StorageConnectorConfiguration();
        config.setContainerName(BUCKET_NAME);

        new S3StorageConnector(s3Client, config);

        assertContains(Level.DEBUG, "S3StorageConnector initialized with bucket: " + BUCKET_NAME);
    }

    private void assertContains(Level level, String expectedMessagePart) {
        List<String> messages = logAppender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertThat(messages).anyMatch(message -> message.contains(expectedMessagePart));
    }

}