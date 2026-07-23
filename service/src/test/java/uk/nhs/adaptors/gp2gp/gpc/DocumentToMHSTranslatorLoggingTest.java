package uk.nhs.adaptors.gp2gp.gpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentToMHSTranslatorLoggingTest {

    private static final String CONVERSATION_ID = "conversation_123";
    private static final String DOCUMENT_ID = "doc_123";
    private static final String MESSAGE_ID = "message_123";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String BASE64_CONTENT = "dGVzdA==";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EhrDocumentMapper ehrDocumentMapper;

    private DocumentToMHSTranslator translator;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        translator = new DocumentToMHSTranslator(objectMapper, ehrDocumentMapper);
        logger = (Logger) LoggerFactory.getLogger(DocumentToMHSTranslator.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void When_TranslatingGpcResponse_Expect_InfoLogWithConversationAndDocument() throws Exception {

        GetGpcDocumentTaskDefinition taskDefinition = createTaskDefinition();
        when(ehrDocumentMapper.generateMhsPayload(any(), any(), any(), any())).thenReturn("<xml/>");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":\"ok\"}");

        String result = translator.translateGpcResponseToMhsOutboundRequestData(taskDefinition, BASE64_CONTENT, CONTENT_TYPE);

        assertEquals("{\"payload\":\"ok\"}", result);
        assertContains(Level.INFO,
            "Translating GPC response to MHS outbound payload for conversation " + CONVERSATION_ID + " and document " + DOCUMENT_ID);
    }

    @Test
    void When_TranslatingFileContent_Expect_InfoLogWithConversationAndDocument() throws Exception {

        GetGpcDocumentTaskDefinition taskDefinition = createTaskDefinition();
        when(ehrDocumentMapper.generateMhsPayload(any(), any(), any(), any())).thenReturn("<xml/>");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":\"ok\"}");

        String result = translator.translateFileContentToMhsOutboundRequestData(taskDefinition, BASE64_CONTENT);

        assertEquals("{\"payload\":\"ok\"}", result);
        assertContains(Level.INFO,
            "Translating file content to MHS outbound payload for conversation " + CONVERSATION_ID + " and document " + DOCUMENT_ID);
    }

    @Test
    void When_JsonSerializationFails_Expect_ErrorLogWithConversationAndDocument() throws Exception {

        GetGpcDocumentTaskDefinition taskDefinition = createTaskDefinition();
        when(ehrDocumentMapper.generateMhsPayload(any(), any(), any(), any())).thenReturn("<xml/>");
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("serialization failed") { });

        assertThrows(IllegalArgumentException.class,
            () -> translator.translateGpcResponseToMhsOutboundRequestData(taskDefinition, BASE64_CONTENT, CONTENT_TYPE));

        assertContains(Level.ERROR,
            "Unable to create outbound MHS message for conversation " + CONVERSATION_ID + " and document " + DOCUMENT_ID);
    }

    private GetGpcDocumentTaskDefinition createTaskDefinition() {
        return GetGpcDocumentTaskDefinition.builder()
            .conversationId(CONVERSATION_ID)
            .documentId(DOCUMENT_ID)
            .messageId(MESSAGE_ID)
            .build();
    }

    private void assertContains(Level level, String expectedMessage) {
        List<String> messages = logAppender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertThat(messages).anyMatch(message -> message.contains(expectedMessage));
    }
}
