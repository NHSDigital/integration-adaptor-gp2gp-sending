package uk.nhs.adaptors.gp2gp.common.configuration;

import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CustomTrustStoreTest {

    public static final int PORT = 8001;
    private static S3Mock s3Mock;
    private static S3Client s3Client;
    private static final String BUCKET_NAME = "test-bucket";
    private static final String TRUSTSTORE_PATH = "test.jks";
    private static final String TRUSTSTORE_PASSWORD = "password";

    private final CustomTrustStore customTrustStore = new CustomTrustStore();

    @BeforeAll
    static void setUp() {
        s3Mock = new S3Mock.Builder().withPort(PORT).withInMemoryBackend().build();
        s3Mock.start();
        System.out.println("S3Mock started at http://localhost:" + PORT);

        s3Client = S3Client.builder()
            .endpointOverride(URI.create("http://localhost:" + PORT))
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

    @AfterAll
    static void tearDown() {
        s3Mock.shutdown();
    }

    @Test
    void trustManagerLoadsSuccessfullyTest() throws NoSuchFieldException, IllegalAccessException {

        Field s3ClientField = CustomTrustStore.class.getDeclaredField("s3Client");
        s3ClientField.setAccessible(true);
        s3ClientField.set(customTrustStore, s3Client);

        String s3Uri = "s3://" + BUCKET_NAME + "/" + TRUSTSTORE_PATH;

        var trustManager = customTrustStore.getCustomDbTrustManager(s3Client.utilities().parseUri(URI.create(s3Uri)), TRUSTSTORE_PASSWORD);

        assertNotNull(trustManager, "Custom TrustManager wasn't loaded successfully!");
    }
}