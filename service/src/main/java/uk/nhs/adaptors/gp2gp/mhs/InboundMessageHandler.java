package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;
import uk.nhs.adaptors.gp2gp.mhs.exception.MessageOutOfOrderException;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnrecognisedInteractionIdException;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnsupportedInteractionException;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import static uk.nhs.adaptors.gp2gp.common.utils.Jms.getJmsMessageTimestamp;
import static uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction.ACKNOWLEDGMENT_REQUEST;
import static uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction.CONTINUE_REQUEST;
import static uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction.EHR_EXTRACT_REQUEST;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageHandler {
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";
    private static final String CONVERSATION_ID_PATH = "/Envelope/Header/MessageHeader/ConversationId";

    private final ObjectMapper objectMapper;
    private final EhrExtractRequestHandler ehrExtractRequestHandler;
    private final XPathService xPathService;
    private final MDCService mdcService;
    private final ProcessFailureHandlingService processFailureHandlingService;

    /**
     * @return True if the message has been processed. Otherwise false.
     */
    @SneakyThrows
    public boolean handle(Message message) throws DataAccessResourceFailureException {
        ParsedInboundMessage parsedMessage = null;
        var messageID = message.getJMSMessageID();

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Inbound message: {}", JmsReader.readMessage(message));
            }
            parsedMessage = parseMessage(message);
            if (!processFailureHandlingService.hasProcessFailed(parsedMessage.getConversationId())) {
                handleInboundMessage(parsedMessage);
            } else {
                LOGGER.warn(
                    "Aborting message handling - the process has already failed for conversation_id: {}.",
                    parsedMessage.getConversationId()
                );
            }
            return true;

        } catch (MessageOutOfOrderException | UnrecognisedInteractionIdException | UnsupportedInteractionException e) {
            LOGGER.error("An error occurred while handing MHS inbound message id: {}", messageID, e);
            return false;
        } catch (DataAccessResourceFailureException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("An error occurred while handing MHS inbound message id: {}", messageID, e);
            return handleMessageProcessingError(parsedMessage);
        }
    }

    private InboundMessage unmarshallMessage(Message message) {
        try {
            var body = JmsReader.readMessage(message);
            return objectMapper.readValue(body, InboundMessage.class);
        } catch (JMSException e) {
            throw new InvalidInboundMessageException("Unable to read the content of the inbound MHS message", e);
        } catch (JsonProcessingException e) {
            throw new InvalidInboundMessageException("Content of the inbound MHS message is not valid JSON", e);
        }
    }

    private void handleInboundMessage(ParsedInboundMessage inboundMessage) {
        String conversationId = inboundMessage.getConversationId();
        mdcService.applyConversationId(conversationId);
        String interactionId = inboundMessage.getInteractionId();

        if (EHR_EXTRACT_REQUEST.getInteractionId().equals(interactionId)) {
            ehrExtractRequestHandler.handleStart(
                inboundMessage.getEbXMLDocument(),
                inboundMessage.getPayloadDocument(),
                inboundMessage.getMessageTimestamp()
            );
        } else if (CONTINUE_REQUEST.getInteractionId().equals(interactionId)) {
            ehrExtractRequestHandler.handleContinue(conversationId, inboundMessage.getRawPayload());
        } else if (ACKNOWLEDGMENT_REQUEST.getInteractionId().equals(interactionId)) {
            ehrExtractRequestHandler.handleAcknowledgement(conversationId, inboundMessage.getPayloadDocument());
        } else {
            throw new UnsupportedInteractionException(interactionId);
        }
    }

    private ParsedInboundMessage parseMessage(Message message) {
        InboundMessage inboundMessage = unmarshallMessage(message);

        var ebXmlDocument = getMessageEnvelope(inboundMessage);
        var payloadDocument = getMessagePayload(inboundMessage);
        var conversationId = getConversationId(ebXmlDocument);
        var interactionId = getInteractionId(ebXmlDocument);
        var messageTimestamp = getJmsMessageTimestamp(message);

        return ParsedInboundMessage.builder()
            .ebXMLDocument(ebXmlDocument)
            .payloadDocument(payloadDocument)
            .rawPayload(inboundMessage.getPayload())
            .conversationId(conversationId)
            .interactionId(interactionId)
            .messageTimestamp(messageTimestamp)
            .build();
    }

    @SneakyThrows
    private boolean handleMessageProcessingError(ParsedInboundMessage parsedMessage) {
        if (parsedMessage != null) {
            return processFailureHandlingService.failProcess(
                parsedMessage.getConversationId(),
                "18",
                "There has been an error when processing the message",
                this.getClass().getSimpleName()
            );
        } else {
            return false;
        }
    }

    private Document getMessageEnvelope(InboundMessage inboundMessage) {
        try {
            return xPathService.parseDocumentFromXml(inboundMessage.getEbXML());
        } catch (SAXException e) {
            throw new InvalidInboundMessageException("Unable to parse the XML envelope (ebxml) of the inbound MHS message", e);
        }
    }

    private Document getMessagePayload(InboundMessage inboundMessage) {
        try {
            return xPathService.parseDocumentFromXml(inboundMessage.getPayload());
        } catch (SAXException e) {
            throw new InvalidInboundMessageException("Unable to parse the XML payload of the inbound MHS message", e);
        }
    }

    private String getInteractionId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, ACTION_PATH);
    }

    private String getConversationId(Document ebXmlDocument) {
        return xPathService.getNodeValue(ebXmlDocument, CONVERSATION_ID_PATH);
    }
}
