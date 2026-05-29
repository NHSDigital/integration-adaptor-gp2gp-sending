package uk.nhs.adaptors.gp2gp.ehr.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith(MongoDBExtension.class)
@SpringBootTest(classes = EhrStatusEndpointsTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EhrStatusRequestsEndpointTests {

    private static final int NUMBER_OF_REQUESTS = 5;
    private static final Instant BASE_TIME = Instant.parse("2024-01-01T10:00:00Z");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    private String ehrStatusRequestsEndpoint;

    private static final Instant[] FROM_DATE_TIMES = new Instant[NUMBER_OF_REQUESTS];
    private static final Instant[] TO_DATE_TIMES = new Instant[NUMBER_OF_REQUESTS];
    private static final String[] TO_ASIDS = new String[NUMBER_OF_REQUESTS];
    private static final String[] FROM_ASIDS = new String[NUMBER_OF_REQUESTS];
    private static final String[] TO_ODSS = new String[NUMBER_OF_REQUESTS];
    private static final String[] FROM_ODSS = new String[NUMBER_OF_REQUESTS];
    private static final String[] CONVERSATION_IDS = new String[NUMBER_OF_REQUESTS];

    @BeforeEach
    public void setUp() {
        ehrStatusRequestsEndpoint = "http://localhost:" + port + "/requests";
        ehrExtractStatusRepository.deleteAll();

        for (var i = 0; i < NUMBER_OF_REQUESTS; i++) {
            var conversationId = UUID.randomUUID().toString();
            var fromAsid = UUID.randomUUID().toString();
            var toAsid = UUID.randomUUID().toString();
            var fromOds = UUID.randomUUID().toString();
            var toOds = UUID.randomUUID().toString();
            var updatedAt = BASE_TIME.plusSeconds(i * 60L);

            ehrExtractStatusRepository.save(createFailedStatus(conversationId, fromAsid, toAsid, fromOds, toOds, updatedAt));

            FROM_DATE_TIMES[i] = updatedAt;
            TO_DATE_TIMES[i] = updatedAt;
            FROM_ASIDS[i] = fromAsid;
            TO_ASIDS[i] = toAsid;
            FROM_ODSS[i] = fromOds;
            TO_ODSS[i] = toOds;
            CONVERSATION_IDS[i] = conversationId;
        }
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoFiltersAreApplied_Expect_StatusEndpointReturnsAllExpectedResponses() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(NUMBER_OF_REQUESTS);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoToDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // If we take the from timestamp of the second created item we should get n - 1 responses where n is the number of data items
        // created for these tests;
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[1])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(NUMBER_OF_REQUESTS - 1);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndNoFromDateFilterIsApplied_Expect_StatusEndpointReturnsTheExpectedRangeOfResponses() {

        // If we take the to timestamp of the first created item we should only get the first data item from our test data
        var queryRequest = EhrStatusRequestQuery.builder()
            .toDateTime(TO_DATE_TIMES[0])
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[0], responseEntity);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndBothToAndFromDateFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        // We are excluding the first and last data item in our test data meaning we should get n-2 results where n is the number of test
        // data items
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[1])
            .toDateTime(TO_DATE_TIMES[TO_DATE_TIMES.length - 2])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(NUMBER_OF_REQUESTS - 2);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndFromASIDFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .fromAsid(FROM_ASIDS[1])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[1], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndToASIDFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toAsid(TO_ASIDS[3])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[3], responseEntity);
    }

    @Test
    public void When_EhrStatusEndpointHasContentAndFromODSFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .fromOdsCode(FROM_ODSS[1])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[1], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndToODSFilterIsApplied_Expect_ExpectedRangeOfResponsesReturned() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toOdsCode(TO_ODSS[4])
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[4], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndMultipleFiltersApplied_Expect_ExpectedRangeOfResponsesReturned() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[0])
            .toDateTime(TO_DATE_TIMES[NUMBER_OF_REQUESTS - 1])
            .toAsid(TO_ASIDS[3])
            .fromOdsCode(FROM_ODSS[3])
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertOneResponseWithConversationId(CONVERSATION_IDS[3], responseEntity);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void When_EhrStatusEndpointHasContentAndFiltersHaveNoResult_Expect_StatusEndpointReturnsA204Response() {
        var queryRequest = EhrStatusRequestQuery.builder()
            .fromDateTime(FROM_DATE_TIMES[0])
            .toDateTime(TO_DATE_TIMES[NUMBER_OF_REQUESTS - 1])
            .toAsid(TO_ASIDS[3])
            .fromOdsCode(FROM_ODSS[2])
            .build();

        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void When_EhrStatusEndpointResponseHasNoContent_Expect_StatusEndpointReturnsA204Response() {

        var queryRequest = EhrStatusRequestQuery.builder()
            .toDateTime(FROM_DATE_TIMES[0].minus(Duration.ofDays(1)))
            .build();
        var responseEntity = restTemplate.postForEntity(ehrStatusRequestsEndpoint, queryRequest, EhrStatusRequest[].class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void assertOneResponseWithConversationId(String conversationId, ResponseEntity<EhrStatusRequest[]> responseEntity) {
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        EhrStatusRequest[] statusRequests = responseEntity.getBody();
        assertThat(statusRequests).isNotNull();
        assertThat(statusRequests.length).isEqualTo(1);
        assertThat(statusRequests[0].getConversationId()).isEqualTo(conversationId);
    }

    private static EhrExtractStatus createFailedStatus(String conversationId, String fromAsid, String toAsid,
        String fromOds, String toOds, Instant updatedAt) {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(conversationId, "document-" + conversationId);
        ehrExtractStatus.setCreated(updatedAt.minusSeconds(1));
        ehrExtractStatus.setUpdatedAt(updatedAt);
        ehrExtractStatus.getEhrRequest().setFromAsid(fromAsid);
        ehrExtractStatus.getEhrRequest().setToAsid(toAsid);
        ehrExtractStatus.getEhrRequest().setFromOdsCode(fromOds);
        ehrExtractStatus.getEhrRequest().setToOdsCode(toOds);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .received(updatedAt)
            .conversationClosed(updatedAt)
            .errors(List.of(EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                .code("test-error-code")
                .display("test error display")
                .build()))
            .messageRef("message-ref-" + conversationId)
            .build());
        return ehrExtractStatus;
    }
}
