package uk.nhs.adaptors.gp2gp.ehr.status;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith(MongoDBExtension.class)
@SpringBootTest(classes = EhrStatusEndpointsTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class EhrStatusEndpointTest {

    private static final String TO_ASID = "715373337545";
    private static final String FROM_ASID = "276827251543";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    private String ehrStatusEndpoint;

    @BeforeEach
    public void setUp() {
        ehrExtractStatusRepository.deleteAll();
        var conversationId = UUID.randomUUID().toString();
        ehrStatusEndpoint = "http://localhost:" + port + "/ehr-status/" + conversationId;

        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(conversationId);
        ehrExtractStatus.getEhrRequest().setFromAsid(FROM_ASID);
        ehrExtractStatus.getEhrRequest().setToAsid(TO_ASID);
        ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    @Test
    public void When_EhrStatusEndpointHasContent_Expect_StatusEndpointReturnsAsidCodes() {
        EhrStatus status = restTemplate.getForObject(ehrStatusEndpoint, EhrStatus.class);

        assertThat(status.getFromAsid()).isEqualTo(FROM_ASID);
        assertThat(status.getToAsid()).isEqualTo(TO_ASID);
    }
}
