package uk.nhs.adaptors.gp2gp.ehr;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.nhs.adaptors.gp2gp.RandomIdGeneratorServiceStub;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorFactory;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorOptions;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendDocumentTaskExecutorTest {

    private static final int SIZE_THRESHOLD_FOUR = 4;
    private static final int SIZE_OF_EACH_CHUNK = 1;
    private static final int NUMBER_OF_CHUNKS = 5;
    private static final String STORAGE_FILE_NAME = "large_file_which_will_be_split.txt";
    private static final String STORAGE_CONTENT_TYPE_UNUSED = "should-not-be-used";
    private static final String RANDOM_ID = "RANDOM-ID";
    private static final String RANDOM_ODS = "RANDOM-ODS";
    private static final String MESSAGE_ID = "88";
    private static final String TASK_CONTENT_TYPE = "should-be-used";
    @Mock private EhrExtractStatusService ehrExtractStatusService;
    @Mock private DetectDocumentsSentService detectDocumentsSentService;
    @Mock private MhsClient mhsClient;
    @Mock private MhsRequestBuilder mhsRequestBuilder;
    private SendDocumentTaskExecutor sendDocumentTaskExecutor;
    private StorageConnector storageConnector;
    private Gp2gpConfiguration gp2gpConfiguration;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(SendDocumentTaskExecutor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        this.storageConnector = createLocalStorageConnector();
        gp2gpConfiguration = new Gp2gpConfiguration();
        this.sendDocumentTaskExecutor = new SendDocumentTaskExecutor(
                new StorageConnectorService(storageConnector, new ObjectMapper()),
                this.mhsRequestBuilder,
                this.mhsClient,
                new RandomIdGeneratorServiceStub(),
                this.ehrExtractStatusService,
                new ObjectMapper(),
                this.detectDocumentsSentService,
                gp2gpConfiguration,
                new EhrDocumentMapper(new TimestampService(), new RandomIdGeneratorServiceStub())
        );
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @ParameterizedTest
    @MethodSource("chunkTestData")
    void When_ChunkingString_Expect_StringIsProperlySplit(String input, int sizeThreshold, List<String> output) {
        var result = SendDocumentTaskExecutor.chunkBinary(input, sizeThreshold);
        assertThat(result).containsExactlyElementsOf(output);
    }

    @SneakyThrows
    @Test
    void When_DocumentNeedsToBeSplitIntoFiveChunks_Expect_FiveMhsRequestsWithAttachmentsOfContentTypeOctetStream() {

        this.gp2gpConfiguration.setLargeAttachmentThreshold(SIZE_OF_EACH_CHUNK);
        uploadFiveChunkDocumentToStorage();

        this.sendDocumentTaskExecutor.execute(createTaskDefinition());

        verify(mhsRequestBuilder, times(NUMBER_OF_CHUNKS)).buildSendEhrExtractCommonRequest(
            argThat(mhsRequestBodyContainsOctetStreamAttachment()),
            eq(RANDOM_ID),
            eq(RANDOM_ODS),
            any()
        );
        verify(mhsClient, times(NUMBER_OF_CHUNKS + 1)).sendMessageToMHS(any());
    }

    @DisplayName("When_DocumentNeedsToBeSplitInto5Chunks_Expect_"
            + "MhsMessageWith5ExternalAttachmentWithDescriptionContentTypeHeaderFromTaskDefinition")
    @Test
    void When_DocumentNeedsToBeSplitInto5Chunks_Expect_MhsMessageWith5ExternalAttachmentCorrectlySet() {

        this.gp2gpConfiguration.setLargeAttachmentThreshold(SIZE_OF_EACH_CHUNK);
        uploadFiveChunkDocumentToStorage();

        this.sendDocumentTaskExecutor.execute(createTaskDefinition());

        verify(mhsRequestBuilder, times(1)).buildSendEhrExtractCommonRequest(
                argThat(mhsRequestBodyHasExternalAttachmentForEachChunkWithTaskContentType()),
                eq(RANDOM_ID),
                eq(RANDOM_ODS),
                any()
        );
        verify(mhsClient, times(NUMBER_OF_CHUNKS + 1)).sendMessageToMHS(any());
    }

    @Test
    void When_ExecutingSendDocumentTask_Expect_InfoLogForPositiveAcknowledgementFlow() {
        this.gp2gpConfiguration.setLargeAttachmentThreshold(SIZE_OF_EACH_CHUNK);
        uploadFiveChunkDocumentToStorage();

        this.sendDocumentTaskExecutor.execute(createTaskDefinition());

        assertThat(logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.INFO)
            .map(ILoggingEvent::getFormattedMessage)
            .toList())
            .anyMatch(message -> message.contains("Executing beginSendingPositiveAcknowledgement"));
    }

    private SendDocumentTaskDefinition createTaskDefinition() {
        return SendDocumentTaskDefinition.builder()
            .documentName(STORAGE_FILE_NAME)
            .messageId(MESSAGE_ID)
            .fromOdsCode(RANDOM_ODS)
            .conversationId(RANDOM_ID)
            .documentContentType(TASK_CONTENT_TYPE)
            .build();
    }

    @NotNull
    private static ArgumentMatcher<String> mhsRequestBodyContainsOctetStreamAttachment() {
        return mhsRequestBody -> {
            ObjectMapper objectMapper = new ObjectMapper();
            OutboundMessage outboundMessage;
            try {
                outboundMessage = objectMapper.readValue(mhsRequestBody, OutboundMessage.class);
            } catch (JsonProcessingException e) {
                return false;
            }
            if (outboundMessage.getAttachments().isEmpty()) {
                return false;
            }
            return Objects.equals(outboundMessage.getAttachments().getFirst().getContentType(), MimeTypes.OCTET_STREAM);
        };
    }

    @NotNull
    private static ArgumentMatcher<String>
        mhsRequestBodyHasExternalAttachmentForEachChunkWithTaskContentType() {

        return mhsRequestBody -> {
            ObjectMapper objectMapper = new ObjectMapper();
            OutboundMessage outboundMessage;
            try {
                outboundMessage = objectMapper.readValue(mhsRequestBody, OutboundMessage.class);
            } catch (JsonProcessingException e) {
                return false;
            }
            if (outboundMessage.getExternalAttachments() == null
                || outboundMessage.getExternalAttachments().size() != NUMBER_OF_CHUNKS) {
                return false;
            }
            return outboundMessage.getExternalAttachments().stream().allMatch(
                    externalAttachment -> externalAttachment.getDescription().contains("ContentType=" + TASK_CONTENT_TYPE)
            );
        };
    }

    private void uploadFiveChunkDocumentToStorage() {
        byte[] storageDataWrapper = generateStorageDataWrapper();
        this.storageConnector.uploadToStorage(
            new ByteArrayInputStream(storageDataWrapper),
            storageDataWrapper.length,
                STORAGE_FILE_NAME
        );
    }

    private static byte[] generateStorageDataWrapper() {
        String payload = "a".repeat(SIZE_OF_EACH_CHUNK * NUMBER_OF_CHUNKS);
        String attachment = "{\"content_type\":\"" + STORAGE_CONTENT_TYPE_UNUSED + "\",\"is_base64\":false"
                + ",\"description\":\"\",\"payload\":\"" + payload + "\"}";
        String outboundMessage = "{\"payload\": \"\", \"attachments\": [" + attachment + "], \"external_attachments\": []}";
        String encodedData = outboundMessage.replace("\"", "\\\""); // Poor persons JSON encode
        String storageDataWrapper = "{\"type\": \"\", \"conversationId\": \"\", \"taskId\": \"\", \"data\": \"" + encodedData + "\"}";
        return storageDataWrapper.getBytes();
    }

    static Stream<Arguments> chunkTestData() {
        return Stream.of(
            Arguments.of("QWER1234", SIZE_THRESHOLD_FOUR, List.of("QWER", "1234")),
            Arguments.of("QWER", SIZE_THRESHOLD_FOUR, List.of("QWER")),
            Arguments.of("QWE", SIZE_THRESHOLD_FOUR, List.of("QWE")),
            Arguments.of("QWER12", SIZE_THRESHOLD_FOUR, List.of("QWER", "12"))
        );
    }

    @Nullable
    private static StorageConnector createLocalStorageConnector() {
        StorageConnectorFactory storageConnectorFactory = new StorageConnectorFactory();
        StorageConnectorConfiguration configuration = new StorageConnectorConfiguration();
        configuration.setType(StorageConnectorOptions.LOCALMOCK.getStringValue());
        storageConnectorFactory.setConfiguration(configuration);
        return storageConnectorFactory.getObject();
    }
}
