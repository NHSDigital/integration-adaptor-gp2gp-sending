package uk.nhs.adaptors.gp2gp.gpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;

import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpcClientTest {

    private static final String CONVERSATION_ID = "conversation_456";
    private static final String ODS_CODE = "B82051";
    private static final String TO_ASID = "to-asid-123";
    private static final String FROM_ASID = "from-asid-456";
    private static final String GPC_URL_TEMPLATE = "https://gpc.example.com/@ODS_CODE@/fhir";
    private static final String STRUCTURED_RESPONSE = "{\"resourceType\": \"Bundle\"}";
    private static final String DOCUMENT_RESPONSE = "{\"resourceType\": \"Binary\"}";

    @Mock
    private GpcConfiguration gpcConfiguration;

    @Mock
    private GpcRequestBuilder gpcRequestBuilder;

    @InjectMocks
    private GpcClient gpcClient;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUpLogCapture() {
        logger = (Logger) LoggerFactory.getLogger(GpcClient.class);
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
    void When_GetStructuredRecordSucceeds_WithDebugLogging_Expect_DebugAndInfoLogs() {
        GetGpcStructuredTaskDefinition structuredTaskDefinition = createStructuredTaskDefinition();
        WebClient.RequestHeadersSpec<?> mockRequest = mock(WebClient.RequestHeadersSpec.class);

        when(gpcConfiguration.getUrl()).thenReturn(GPC_URL_TEMPLATE);
        when(gpcConfiguration.getMigrateStructuredEndpoint()).thenReturn("/Appointment");
        when(gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition)).thenReturn(null);
        doReturn(mockRequest).when(gpcRequestBuilder).buildGetStructuredRecordRequest(
            any(), any(GetGpcStructuredTaskDefinition.class), any(String.class));
        mockRequestResponse(mockRequest, STRUCTURED_RESPONSE);

        String result = gpcClient.getStructuredRecord(structuredTaskDefinition);

        assertEquals(STRUCTURED_RESPONSE, result);
        assertContains(Level.INFO, "Getting structured record for conversation " + CONVERSATION_ID);
        assertContains(Level.DEBUG, "Built GPC base URL for ODS code: " + ODS_CODE);
        assertContains(Level.DEBUG, "Executing GPC HTTP request");
        assertContains(Level.DEBUG, "Get StructuredRecord response body:");
        assertContains(Level.DEBUG, "Structured record retrieved, duration:");
    }

    @Test
    void When_GetStructuredRecordSucceeds_WithInfoLogging_Expect_SuccessLog() {
        logger.setLevel(Level.INFO);

        GetGpcStructuredTaskDefinition structuredTaskDefinition = createStructuredTaskDefinition();
        WebClient.RequestHeadersSpec<?> mockRequest = mock(WebClient.RequestHeadersSpec.class);

        when(gpcConfiguration.getUrl()).thenReturn(GPC_URL_TEMPLATE);
        when(gpcConfiguration.getMigrateStructuredEndpoint()).thenReturn("/Appointment");
        when(gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition)).thenReturn(null);
        doReturn(mockRequest).when(gpcRequestBuilder).buildGetStructuredRecordRequest(
            any(), any(GetGpcStructuredTaskDefinition.class), any(String.class));
        mockRequestResponse(mockRequest, STRUCTURED_RESPONSE);

        String result = gpcClient.getStructuredRecord(structuredTaskDefinition);

        assertEquals(STRUCTURED_RESPONSE, result);
        assertContains(Level.INFO, "Structured record retrieved successfully for conversation " + CONVERSATION_ID);
    }

    @Test
    void When_GetDocumentRecordSucceeds_WithDebugLogging_Expect_DebugAndInfoLogs() {

        GetGpcDocumentTaskDefinition documentTaskDefinition = createDocumentTaskDefinition();
        WebClient.RequestHeadersSpec<?> mockRequest = mock(WebClient.RequestHeadersSpec.class);

        when(gpcConfiguration.getUrl()).thenReturn(GPC_URL_TEMPLATE);
        doReturn(mockRequest).when(gpcRequestBuilder).buildGetDocumentRecordRequest(
            any(GetGpcDocumentTaskDefinition.class), any(String.class));
        mockRequestResponse(mockRequest, DOCUMENT_RESPONSE);

        String result = gpcClient.getDocumentRecord(documentTaskDefinition);

        assertEquals(DOCUMENT_RESPONSE, result);
        assertContains(Level.INFO, "Getting document record for conversation " + CONVERSATION_ID);
        assertContains(Level.DEBUG, "Built GPC base URL for ODS code: " + ODS_CODE);
        assertContains(Level.DEBUG, "Executing GPC HTTP request");
        assertContains(Level.DEBUG, "Get Document response body:");
        assertContains(Level.DEBUG, "Document record retrieved, duration:");
    }

    @Test
    void When_GetDocumentRecordSucceeds_WithInfoLogging_Expect_SuccessLog() {
        logger.setLevel(Level.INFO);

        GetGpcDocumentTaskDefinition documentTaskDefinition = createDocumentTaskDefinition();
        WebClient.RequestHeadersSpec<?> mockRequest = mock(WebClient.RequestHeadersSpec.class);

        when(gpcConfiguration.getUrl()).thenReturn(GPC_URL_TEMPLATE);
        doReturn(mockRequest).when(gpcRequestBuilder).buildGetDocumentRecordRequest(
            any(GetGpcDocumentTaskDefinition.class), any(String.class));
        mockRequestResponse(mockRequest, DOCUMENT_RESPONSE);

        String result = gpcClient.getDocumentRecord(documentTaskDefinition);

        assertEquals(DOCUMENT_RESPONSE, result);
        assertContains(Level.INFO, "Document record retrieved successfully for conversation " + CONVERSATION_ID);
    }

    @Test
    void When_GetStructuredRecordFails_Expect_ErrorLogs() {

        GetGpcStructuredTaskDefinition structuredTaskDefinition = createStructuredTaskDefinition();
        WebClient.RequestHeadersSpec<?> mockRequest = mock(WebClient.RequestHeadersSpec.class);

        when(gpcConfiguration.getUrl()).thenReturn(GPC_URL_TEMPLATE);
        when(gpcConfiguration.getMigrateStructuredEndpoint()).thenReturn("/Appointment");
        when(gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition)).thenReturn(null);
        doReturn(mockRequest).when(gpcRequestBuilder).buildGetStructuredRecordRequest(
            any(), any(GetGpcStructuredTaskDefinition.class), any(String.class));
        mockRequestFailure(mockRequest);

        assertThrows(RuntimeException.class, () -> gpcClient.getStructuredRecord(structuredTaskDefinition));
        assertContains(Level.ERROR, "GPC HTTP request failed");
        assertContains(Level.ERROR, "Error retrieving structured record for conversation " + CONVERSATION_ID);
    }

    @Test
    void When_GetDocumentRecordFails_Expect_ErrorLogs() {

        GetGpcDocumentTaskDefinition documentTaskDefinition = createDocumentTaskDefinition();
        WebClient.RequestHeadersSpec<?> mockRequest = mock(WebClient.RequestHeadersSpec.class);

        when(gpcConfiguration.getUrl()).thenReturn(GPC_URL_TEMPLATE);
        doReturn(mockRequest).when(gpcRequestBuilder).buildGetDocumentRecordRequest(
            any(GetGpcDocumentTaskDefinition.class), any(String.class));
        mockRequestFailure(mockRequest);

        assertThrows(RuntimeException.class, () -> gpcClient.getDocumentRecord(documentTaskDefinition));
        assertContains(Level.ERROR, "GPC HTTP request failed");
        assertContains(Level.ERROR, "Error retrieving document record for conversation " + CONVERSATION_ID);
    }

    private GetGpcStructuredTaskDefinition createStructuredTaskDefinition() {
        return GetGpcStructuredTaskDefinition.builder()
            .conversationId(CONVERSATION_ID)
            .toOdsCode(ODS_CODE)
            .fromOdsCode("FROM_ODS")
            .toAsid(TO_ASID)
            .fromAsid(FROM_ASID)
            .nhsNumber("1234567890")
            .build();
    }

    private GetGpcDocumentTaskDefinition createDocumentTaskDefinition() {
        return GetGpcDocumentTaskDefinition.builder()
            .conversationId(CONVERSATION_ID)
            .toOdsCode(ODS_CODE)
            .fromOdsCode("FROM_ODS")
            .toAsid(TO_ASID)
            .fromAsid(FROM_ASID)
            .accessDocumentUrl("https://example.com/document")
            .build();
    }

    private void mockRequestResponse(WebClient.RequestHeadersSpec<?> mockRequest, String responseBody) {
        WebClient.ResponseSpec mockResponseSpec = mock(WebClient.ResponseSpec.class);
        when(mockRequest.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
    }

    private void mockRequestFailure(WebClient.RequestHeadersSpec<?> mockRequest) {
        WebClient.ResponseSpec mockResponseSpec = mock(WebClient.ResponseSpec.class);
        when(mockRequest.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Request failed")));
    }

    private void assertContains(Level level, String expectedMessagePart) {
        Assertions.assertThat(messages(level))
            .anyMatch(message -> message.contains(expectedMessagePart));
    }

    private List<String> messages(Level level) {
        return logAppender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
