package uk.nhs.adaptors.gp2gp.common.configuration;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.File;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class CustomTrustStoreTest {

    @Container
    private static final S3MockContainer S3_MOCK = new S3MockContainer("4.7.0");

    private static S3Client s3Client;
    private static final String BUCKET_NAME = "test-bucket";
    private static final String TRUSTSTORE_PATH = "test.jks";
    private static final String TRUSTSTORE_PASSWORD = "password";
    private static CustomTrustStore customTrustStore;

    @BeforeAll
    static void setUp() {
        s3Client = S3Client.builder()
            .endpointOverride(URI.create(S3_MOCK.getHttpEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("accessKey", "secretKey")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .region(Region.EU_WEST_2)
            .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        File trustStoreFile = new File("src/test/resources/test.jks");
        s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(TRUSTSTORE_PATH).build(),
                           software.amazon.awssdk.core.sync.RequestBody.fromFile(trustStoreFile));
    }

    @BeforeAll
    static void setup() {
        customTrustStore = new CustomTrustStore(s3Client);
    }

    @AfterAll
    static void tearDown() {
        customTrustStore = null;
    }

    @Test
    void trustManagerLoadsSuccessfullyTest() {

        String s3Uri = "s3://" + BUCKET_NAME + "/" + TRUSTSTORE_PATH;

        var trustManager = customTrustStore.getCustomDbTrustManager(s3Client.utilities().parseUri(URI.create(s3Uri)), TRUSTSTORE_PASSWORD);

        assertNotNull(trustManager, "Custom TrustManager wasn't loaded successfully!");
    }
}