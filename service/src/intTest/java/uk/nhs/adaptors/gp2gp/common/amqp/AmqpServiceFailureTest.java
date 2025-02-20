package uk.nhs.adaptors.gp2gp.common.amqp;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import jakarta.jms.JMSException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.common.task.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.InboundMessageHandlingTest;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.util.ProcessDetectionService;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class AmqpServiceFailureTest {

    private static final String CONVERSATION_ID_PLACEHOLDER = "{{conversationId}}";
    private static final String EHR_MESSAGE_REF_PLACEHOLDER = "{{ehrMessageRef}}";
    private static final String EBXML_PATH_CONTINUE_MESSAGE = "/continuemessage/COPC_IN000001UK01_ebxml.txt";
    private static final String PAYLOAD_PATH_CONTINUE_MESSAGE = "/continuemessage/COPC_IN000001UK01_payload.txt";
    private static final String EBXML_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_ebxml.txt";
    private static final String PAYLOAD_PATH_REQUEST_MESSAGE = "/requestmessage/RCMR_IN010000UK05_payload.txt";
    private static final String EBXML_PATH_FINAL_ACK_MESSAGE = "/finalAckMessage/MCCI_IN010000UK13_ebxml.txt";
    private static final String PAYLOAD_PATH_FINAL_ACK_MESSAGE = "/finalAckMessage/MCCI_IN010000UK1313_payload.txt";
    private static final String INBOUND_QUEUE_NAME = "gp2gpInboundQueue";
    private static final String TASK_QUEUE_NAME = "gp2gpTaskQueue";
    private static final String DLQ_PREFIX = "DLQ.";
    private static final int JMS_RECEIVE_TIMEOUT = 60000;
    private static final Duration THREE_SECONDS = Duration.ofSeconds(3);
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration THIRTY_SECONDS = Duration.ofSeconds(30);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private JmsTemplate inboundJmsTemplate;
    @Autowired
    private ProcessDetectionService processDetectionService;
    @MockitoSpyBean
    private ProcessFailureHandlingService processFailureHandlingService;
    @MockitoSpyBean
    private SendEhrExtractCoreTaskExecutor sendEhrExtractCoreTaskExecutor;
    private String conversationId;

    @BeforeEach
    public void setUp() {

        inboundJmsTemplate.setDefaultDestinationName(INBOUND_QUEUE_NAME);
        conversationId = UUID.randomUUID().toString();
        inboundJmsTemplate.setReceiveTimeout(JMS_RECEIVE_TIMEOUT);
    }

    @Test
    public void When_TransferProcessed_WithNormalConditions_Expect_TransferComplete() {
        attemptTransfer();

        assertThat(transferComplete()).isTrue();
    }

    @Test
    public void When_TransferProcessed_WithDataBaseDown_Expect_TransferProcessedWhenDataBaseRecovers() {
        // ensure method throws (at least) one more time than the value of GP2GP_AMQP_MAX_REDELIVERIES (default 3)
        when(processFailureHandlingService.hasProcessFailed(conversationId))
            .thenThrow(DataAccessResourceFailureException.class)
            .thenThrow(DataAccessResourceFailureException.class)
            .thenThrow(DataAccessResourceFailureException.class)
            .thenThrow(DataAccessResourceFailureException.class)
            .thenThrow(DataAccessResourceFailureException.class)
            .thenCallRealMethod();

        attemptTransfer();

        assertThat(transferComplete()).isTrue();
    }

    @Test
    public void When_TransferProcessed_WithMhsOutboundServiceDown_Expect_TransferProcessedWhenServiceRecovers() {
        // ensure method throws (at least) one more time than the value of GP2GP_AMQP_MAX_REDELIVERIES (default 3)
        doThrow(MhsConnectionException.class)
            .doThrow(MhsConnectionException.class)
            .doThrow(MhsConnectionException.class)
            .doThrow(MhsConnectionException.class)
            .doThrow(MhsConnectionException.class)
            .doCallRealMethod()
            .when(sendEhrExtractCoreTaskExecutor).execute(any());

        attemptTransfer();

        assertThat(transferComplete()).isTrue();
    }

    @Test
    public void When_TransferProcessed_WithMhsOutboundServiceServerError_Expect_TransferFailed() {
        // ensure method throws (at least) one more time than the value of GP2GP_AMQP_MAX_REDELIVERIES (default 3)
        doThrow(MhsServerErrorException.class)
            .doThrow(MhsServerErrorException.class)
            .when(sendEhrExtractCoreTaskExecutor).execute(any());

        sendInboundMessageToQueue(PAYLOAD_PATH_REQUEST_MESSAGE, EBXML_PATH_REQUEST_MESSAGE);

        await().atMost(ONE_MINUTE).until(this::processFailed);
        assertThat(processFailed()).isTrue();

    }

    @Test
    public void When_TransferProcessing_WithTaskHandlerException_Expect_MessagePutOnDLQ() throws JMSException, JsonProcessingException {
        doThrow(TaskHandlerException.class)
            .when(sendEhrExtractCoreTaskExecutor).execute(any());

        sendInboundMessageToQueue(PAYLOAD_PATH_REQUEST_MESSAGE, EBXML_PATH_REQUEST_MESSAGE);

        var dlqMessage = Optional.ofNullable(inboundJmsTemplate.receive(DLQ_PREFIX + TASK_QUEUE_NAME)).orElseThrow();
        var body = JmsReader.readMessage(dlqMessage);
        var dlqTaskDefinition = objectMapper.readValue(body, SendEhrExtractCoreTaskDefinition.class);

        assertThat(dlqTaskDefinition.getConversationId()).isEqualTo(conversationId);
    }

    @Test
    public void When_TransferProcessing_WithUnexpectedExceptionDuringRequest_Expect_MessagePutOnDLQ() throws JsonProcessingException,
        JMSException {
        doThrow(RuntimeException.class)
            .when(processFailureHandlingService).hasProcessFailed(conversationId);

        var inboundMessage = sendInboundMessageToQueue(PAYLOAD_PATH_REQUEST_MESSAGE, EBXML_PATH_REQUEST_MESSAGE);

        var dlqMessage = Optional.ofNullable(inboundJmsTemplate.receive(DLQ_PREFIX + INBOUND_QUEUE_NAME)).orElseThrow();
        var body = JmsReader.readMessage(dlqMessage);
        var dlqInboundMessage = objectMapper.readValue(body, InboundMessage.class);

        assertThat(dlqInboundMessage).isEqualTo(inboundMessage);
    }

    @Test
    public void When_TransferProcessing_WithUnexpectedExceptionDuringContinue_Expect_ProcessFailed() {

        sendInboundMessageToQueue(PAYLOAD_PATH_REQUEST_MESSAGE, EBXML_PATH_REQUEST_MESSAGE);

        await()
            .atMost(ONE_MINUTE)
            .pollInterval(THREE_SECONDS)
            .until(this::awaitingContinue);

        when(processFailureHandlingService.hasProcessFailed(conversationId))
            .thenThrow(RuntimeException.class);

        sendInboundMessageToQueue(PAYLOAD_PATH_CONTINUE_MESSAGE, EBXML_PATH_CONTINUE_MESSAGE);

        await().atMost(THIRTY_SECONDS).until(this::processFailed);
        assertThat(processFailed()).isTrue();
    }

    @Test
    public void When_TransferProcessing_WithUnexpectedExceptionDuringFinalAck_Expect_ProcessFailed() {

        sendInboundMessageToQueue(PAYLOAD_PATH_REQUEST_MESSAGE, EBXML_PATH_REQUEST_MESSAGE);

        await()
            .atMost(ONE_MINUTE)
            .pollInterval(THREE_SECONDS)
            .until(this::awaitingContinue);

        sendInboundMessageToQueue(PAYLOAD_PATH_CONTINUE_MESSAGE, EBXML_PATH_CONTINUE_MESSAGE);

        await()
            .atMost(THIRTY_SECONDS)
            .until(this::awaitingAck);

        when(processFailureHandlingService.hasProcessFailed(conversationId))
            .thenThrow(RuntimeException.class);

        sendFinalAckToQueue();

        await().atMost(THIRTY_SECONDS).until(this::processFailed);
        assertThat(processFailed()).isTrue();
    }

    private void attemptTransfer() {
        sendInboundMessageToQueue(PAYLOAD_PATH_REQUEST_MESSAGE, EBXML_PATH_REQUEST_MESSAGE);

        await()
            .atMost(ONE_MINUTE)
            .pollInterval(THREE_SECONDS)
            .until(this::awaitingContinue);

        sendInboundMessageToQueue(PAYLOAD_PATH_CONTINUE_MESSAGE, EBXML_PATH_CONTINUE_MESSAGE);

        await()
            .atMost(THIRTY_SECONDS)
            .until(this::awaitingAck);

        sendFinalAckToQueue();

        await()
            .atMost(THIRTY_SECONDS)
            .until(this::transferComplete);
    }

    private boolean awaitingAck() {
        return processDetectionService.awaitingAck(conversationId);
    }

    private boolean awaitingContinue() {
        return processDetectionService.awaitingContinue(conversationId);
    }

    private boolean transferComplete() {
        return processDetectionService.transferComplete(conversationId);
    }

    private boolean processFailed() {
        return processDetectionService.processFailed(conversationId);
    }

    private EhrExtractStatus readEhrExtractStatusFromDb() {
        return ehrExtractStatusRepository.findByConversationId(conversationId).orElseThrow();
    }

    private InboundMessage sendInboundMessageToQueue(String payloadPartPath, String ebxmlPartPath) {
        var inboundMessage = createInboundMessage(payloadPartPath, ebxmlPartPath);
        inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));

        return inboundMessage;
    }

    private void sendFinalAckToQueue() {
        var inboundMessage = createFinalAckMessage();
        inboundJmsTemplate.send(session -> session.createTextMessage(parseMessageToString(inboundMessage)));
    }

    private InboundMessage createInboundMessage(String payloadPartPath, String ebxmlPathPart) {
        var inboundMessage = new InboundMessage();
        var payload = readResourceAsString(payloadPartPath);
        var ebXml = readResourceAsString(ebxmlPathPart).replace(CONVERSATION_ID_PLACEHOLDER, conversationId);
        inboundMessage.setPayload(payload);
        inboundMessage.setEbXML(ebXml);
        return inboundMessage;
    }

    private InboundMessage createFinalAckMessage() {
        var ehrExtractStatus = readEhrExtractStatusFromDb();
        var ehrMessageRef = ehrExtractStatus.getEhrExtractMessageId();
        var inboundMessage = new InboundMessage();
        var payload = readResourceAsString(AmqpServiceFailureTest.PAYLOAD_PATH_FINAL_ACK_MESSAGE).replace(EHR_MESSAGE_REF_PLACEHOLDER,
            ehrMessageRef);
        var ebXml = readResourceAsString(AmqpServiceFailureTest.EBXML_PATH_FINAL_ACK_MESSAGE).replace(CONVERSATION_ID_PLACEHOLDER,
            conversationId);
        inboundMessage.setPayload(payload);
        inboundMessage.setEbXML(ebXml);
        return inboundMessage;
    }

    @SneakyThrows
    private String parseMessageToString(InboundMessage inboundMessage) {
        return objectMapper.writeValueAsString(inboundMessage);
    }

    @SneakyThrows
    private static String readResourceAsString(String path) {
        try (InputStream is = InboundMessageHandlingTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new FileNotFoundException(path);
            }
            return IOUtils.toString(is, UTF_8);
        }
    }
}
