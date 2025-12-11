package uk.nhs.adaptors.gp2gp.ehr;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import jakarta.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileCopyUtils;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageHandler;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;


@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class IllogicalMessageComponentTest {
    private static final XPathService SERVICE = new XPathService();

    @Mock
    private TaskDispatcher taskDispatcher;
    @InjectMocks
    private InboundMessageHandler inboundMessageHandler;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private Message message;

    @Value("classpath:illogicalmessage/COPC_IN000001UK01_ebxml.txt")
    private Resource continueResponseEbxml;
    @Value("classpath:illogicalmessage/MCCI_IN010000UK13_ebxml.txt")
    private Resource acknowledgementResponseEbxml;


    @Test
    public void When_ContinueReceivedToNonExistingEhrExtractStatus_Expect_ErrorThrown() {
        String continueEbxml = asString(continueResponseEbxml);

        mockIncomingMessage(continueEbxml);

        assertFalse(inboundMessageHandler.handle(message));

        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_AcknowledgementReceivedToNonExistingEhrExtractStatus_Expect_ErrorThrown() {
        String acknowledgementEbxml = asString(acknowledgementResponseEbxml);

        mockAcknowledgementMessage(acknowledgementEbxml);

        assertFalse(inboundMessageHandler.handle(message));

        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_ContinueReceivedOutOfOrderExtractCoreNotSent_Expect_ErrorThrown() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String continueEbxml = asString(continueResponseEbxml);

        mockIncomingMessage(continueEbxml);

        assertFalse(inboundMessageHandler.handle(message));

        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_AcknowledgementReceivedOutOfOrderAcknowledgmentNotSent_Expect_ErrorThrown() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String acknowledgementEbxml = asString(acknowledgementResponseEbxml);

        mockAcknowledgementMessage(acknowledgementEbxml);

        assertFalse(inboundMessageHandler.handle(message));

        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_DuplicateEhrRequestReceived_Expect_SkippedNoDatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();

        ehrExtractStatusRepository.save(ehrExtractStatus);
        inboundMessageHandler.handle(message);

        var firstEhrStatus =
                ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        inboundMessageHandler.handle(message);

        var secondEhrStatus =
                ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(firstEhrStatus.getUpdatedAt()).isEqualTo(secondEhrStatus.getUpdatedAt());
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_DuplicateContinueReceived_Expect_SkippedNoDatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatus.setEhrContinue(EhrExtractStatus.EhrContinue.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String continueEbxml = asString(continueResponseEbxml);

        mockIncomingMessage(continueEbxml);

        inboundMessageHandler.handle(message);
        var firstEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        inboundMessageHandler.handle(message);
        var secondEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(firstEhrStatus.getUpdatedAt()).isEqualTo(secondEhrStatus.getUpdatedAt());
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_DuplicateAcknowledgementSentTwice_Expect_SkippedNoDatabaseUpdatedn() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatus.setAckToRequester(EhrExtractStatus.AckToRequester.builder().build());
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String acknowledgementEbxml = asString(acknowledgementResponseEbxml);

        mockAcknowledgementMessage(acknowledgementEbxml);

        inboundMessageHandler.handle(message);
        var firstEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        inboundMessageHandler.handle(message);
        var secondEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(firstEhrStatus.getUpdatedAt()).isEqualTo(secondEhrStatus.getUpdatedAt());
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    @SneakyThrows
    public void When_UnsupportedMessageSent_Expect_ErrorThrown() {
        assertFalse(inboundMessageHandler.handle(message));

        verify(taskDispatcher, never()).createTask(any());
    }

    private static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SneakyThrows
    private void mockIncomingMessage(String ebxml) {
        SERVICE.parseDocumentFromXml(ebxml);
    }

    @SneakyThrows
    private void mockAcknowledgementMessage(String ebxml) {
        mockIncomingMessage(ebxml);
    }
}
