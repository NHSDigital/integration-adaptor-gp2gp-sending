package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.nhs.adaptors.gp2gp.common.exception.MaximumExternalAttachmentsException;
import uk.nhs.adaptors.gp2gp.common.exception.RetryLimitReachedException;
import uk.nhs.adaptors.gp2gp.mhs.configuration.MhsConfiguration;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class, MongoDBExtension.class})
@DirtiesContext
public class MhsWebClientTest {

    private static final String TEST_BODY = "test body";
    private static final String TEST_CONVERSATION_ID = "test conversation id";
    private static final String TEST_FROM_ODS_CODE = "test from ods code";
    private static final String TEST_MESSAGE_ID = "test message id";
    private static final int FOUR = 4;

    private static MockWebServer mockWebServer;

    @Autowired
    private MhsRequestBuilder mhsRequestBuilder;
    @Autowired
    private MhsClient mhsClient;

    @MockitoSpyBean
    private MhsConfiguration mhsConfiguration;

    private static Stream<Arguments> maxAttachmentsValidationErrors() {
        return Stream.of(
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['Longer than maximum length 99.']}"),
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['unknown issue','Longer than "
                + "maximum length 99.']}"),
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': "
                + "['unknown issue','Longer than maximum length 99.'], 'internal_attachments': ['unknown issue']}}")
        );
    }

    private static Stream<Arguments> otherValidationErrors() {
        return Stream.of(
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['unknown issue']}"),
            Arguments.of("400: Invalid request. Validation errors: {'internal_attachments': ['Longer than maximum length 99.']}"),
            Arguments.of("400: Invalid request. Validation errors: {'external_attachments': ['unknown issue'], "
                + "'internal_attachments': ['Longer than maximum length 99.']}")
        );
    }

    @BeforeEach
    public void initialise() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        when(mhsConfiguration.getUrl()).thenReturn(baseUrl);
    }

    @AfterEach
    public void tearDown() {
        mockWebServer.close();
    }

    @ParameterizedTest
    @MethodSource("maxAttachmentsValidationErrors")
    public void When_SendMessageToMHS_With_HttpStatus400AndMaxExternalAttachments_Expect_CorrectException(String body) {
        var response = new MockResponse.Builder()
            .code(BAD_REQUEST.value())
            .addHeader("Content-Type", "text/plain")
            .body(body)
            .build();

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(MaximumExternalAttachmentsException.class);
    }

    @ParameterizedTest
    @MethodSource("otherValidationErrors")
    public void When_SendMessageToMHS_With_HttpStatus400AndOtherValidationErrors_Expect_CorrectException(String body) {
        var response = new MockResponse.Builder()
            .code(BAD_REQUEST.value())
            .addHeader("Content-Type", "text/plain")
            .body(body)
            .build();

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(InvalidOutboundMessageException.class);
    }


    @Test
    public void When_SendMessageToMHS_With_HttpStatus404_Expect_IllegalStateException() {
        var response =  new MockResponse.Builder()
            .code(NOT_FOUND.value())
            .build();

        mockWebServer.enqueue(response);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void When_SendMessageToMHS_With_NoResponse_Expect_RetryExceptionWithTimeoutRootCause() {
        var response =  new MockResponse.Builder()
            .bodyDelay(1, TimeUnit.HOURS)
            .headersDelay(1, TimeUnit.HOURS)
            .build();

        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(response);
        }

        var request = mhsRequestBuilder.buildSendEhrExtractCommonRequest(TEST_BODY, TEST_CONVERSATION_ID,
            TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() ->
            mhsClient.sendMessageToMHS(request))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(TimeoutException.class);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);

    }

    @Test
    public void When_SendMessageToMHS_With_ResponseOn404SecondAttempt_Expect_RetryBeforeIllegalStateException() {
        var response1 =  new MockResponse.Builder()
            .bodyDelay(1, TimeUnit.HOURS)
            .headersDelay(1, TimeUnit.HOURS)
            .build();
        var response2 = new MockResponse.Builder()
            .code(NOT_FOUND.value())
            .build();

        mockWebServer.enqueue(response1);
        mockWebServer.enqueue(response2);

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() -> mhsClient.sendMessageToMHS(request))
            .isInstanceOf(IllegalStateException.class);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    public void When_SendMessageToMHS_With_HttpStatus5xx_Expect_RetryExceptionWithMhsServerErrorRootCause() {
        var response = new MockResponse.Builder()
            .code(INTERNAL_SERVER_ERROR.value())
            .build();

        for (int i = 0; i < FOUR; i++) {
            mockWebServer.enqueue(response);
        }

        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(TEST_BODY,
            TEST_CONVERSATION_ID, TEST_FROM_ODS_CODE, TEST_MESSAGE_ID);

        assertThatThrownBy(() ->
            mhsClient.sendMessageToMHS(request))
            .isInstanceOf(RetryLimitReachedException.class)
            .hasMessage("Retries exhausted: 3/3")
            .hasRootCauseInstanceOf(MhsServerErrorException.class)
            .hasRootCauseMessage("The following error occurred during MHS_OUTBOUND request: 500 INTERNAL_SERVER_ERROR");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(FOUR);
    }
}
