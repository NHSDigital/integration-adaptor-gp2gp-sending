package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
class AgentDirectoryMapperTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String NHS_NUMBER = "9465701459";
    private static final String AGENT_DIRECTORY_FOLDER = "/ehr/mapper/agent-directory/";
    private static final String INPUT_AGENT_DIRECTORY = AGENT_DIRECTORY_FOLDER + "input-agent-directory-bundle.json";
    private static final String INPUT_AGENT_DIRECTORY_WITHOUT_MANAGING_ORGANIZATION_REFERENCE =
            AGENT_DIRECTORY_FOLDER + "without-patient-managing-organization-reference.json";
    private static final String INPUT_AGENT_DIRECTORY_WITHOUT_MANAGING_ORGANIZATION_RESOURCE =
            AGENT_DIRECTORY_FOLDER + "without-patient-managing-organization-resource.json";
    private static final String EXPECTED_AGENT_DIRECTORY = AGENT_DIRECTORY_FOLDER + "expected-agent-directory.xml";
    private static final String EXPECTED_AGENT_DIRECTORY_AGENT_PERSON_AS_ORGANIZATION =
            AGENT_DIRECTORY_FOLDER + "expected-with-agent-person-as-organization.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private AgentPersonMapper agentPersonMapper;

    private AgentDirectoryMapper agentDirectoryMapper;
    private FhirParseService fhirParseService;
    private MessageContext messageContext;

    @BeforeEach
    void setUp() {
        messageContext = new MessageContext(randomIdGeneratorService);
        agentDirectoryMapper = new AgentDirectoryMapper(messageContext, agentPersonMapper);
        fhirParseService = new FhirParseService();
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
    }

    @AfterEach
    void tearDown() {
        messageContext.resetMessageContext();
        messageContext = null;
        agentDirectoryMapper = null;
        fhirParseService = null;
        reset(randomIdGeneratorService, agentPersonMapper);
    }

    private Answer<String> answerWithObjectId() {
        return invocation -> {
            var agentKey = invocation.getArgument(0, AgentDirectory.AgentKey.class);
            return String.format("<!--Mocked agentPerson for: %s %s -->",
                    agentKey.getPractitionerReference(),
                    agentKey.getOrganizationReference());
        };
    }

    private Bundle parseBundle(String path) {
        var jsonInput = ResourceTestFileUtils.getFileContent(path);
        return fhirParseService.parseResource(jsonInput, Bundle.class);
    }

    private String readExpectedOutput(String path) {
        return ResourceTestFileUtils.getFileContent(path);
    }

    private void initializeMessageContextWithAgentKeys(Bundle bundle) {
        messageContext.initialize(bundle);
        messageContext.getAgentDirectory().getAgentRef(
                buildReference(ResourceType.Practitioner, "11112222"),
                buildReference(ResourceType.Organization, "33334444")
        );
        messageContext.getAgentDirectory().getAgentRef(
                buildReference(ResourceType.Practitioner, "55556666"),
                buildReference(ResourceType.Organization, "77778888")
        );
    }

    @Test
    void When_MappingAgentDirectory_Expect_CorrectOutputFromMapper() {
        when(agentPersonMapper.mapAgentPerson(any(), any())).thenAnswer(answerWithObjectId());

        var bundle = parseBundle(INPUT_AGENT_DIRECTORY);
        initializeMessageContextWithAgentKeys(bundle);

        var expectedOutput = readExpectedOutput(EXPECTED_AGENT_DIRECTORY);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);

        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    @Test
    void When_MappingAgentDirectoryWithoutPatientManagingOrganizationReference_Expect_Exception() {
        var bundle = parseBundle(INPUT_AGENT_DIRECTORY_WITHOUT_MANAGING_ORGANIZATION_REFERENCE);
        initializeMessageContextWithAgentKeys(bundle);

        assertThatThrownBy(() -> agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER))
                .isInstanceOf(EhrMapperException.class)
                .hasMessage("The ASR bundle does not contain a Patient resource with the correct identifier and managingOrganization");
    }

    @Test
    void When_MappingAgentDirectoryWithoutPatientManagingOrganizationResource_Expect_Exception() {
        var bundle = parseBundle(INPUT_AGENT_DIRECTORY_WITHOUT_MANAGING_ORGANIZATION_RESOURCE);
        initializeMessageContextWithAgentKeys(bundle);

        assertThatThrownBy(() -> agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER))
                .isInstanceOf(EhrMapperException.class)
                .hasMessage("The ASR bundle does not contain a Patient resource with the correct identifier and managingOrganization");
    }

    @Test
    void When_MappingAgentDirectoryWithPatientManagingOrganizationInAgentKeys_Expect_AgentPersonNotDuplicated() {
        when(agentPersonMapper.mapAgentPerson(any(), any())).thenAnswer(answerWithObjectId());

        var bundle = parseBundle(INPUT_AGENT_DIRECTORY);
        initializeMessageContextWithAgentKeys(bundle);
        messageContext.getAgentDirectory().getAgentId(
                buildReference(ResourceType.Organization, TEST_ID)
        );

        var expectedOutput = readExpectedOutput(EXPECTED_AGENT_DIRECTORY);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);

        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    @Test
    void When_MappingAgentKeysWithoutAgentKeys_Expect_CorrectOutputFromMapper() {
        var bundle = parseBundle(INPUT_AGENT_DIRECTORY);
        messageContext.initialize(bundle);

        var expectedOutput = readExpectedOutput(EXPECTED_AGENT_DIRECTORY_AGENT_PERSON_AS_ORGANIZATION);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);

        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }
}
