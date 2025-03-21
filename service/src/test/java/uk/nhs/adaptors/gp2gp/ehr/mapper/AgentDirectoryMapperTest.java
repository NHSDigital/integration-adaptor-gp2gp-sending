package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
public class AgentDirectoryMapperTest {

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
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        lenient().when(agentPersonMapper.mapAgentPerson(any(), any())).thenAnswer(answerWithObjectId());

        agentDirectoryMapper = new AgentDirectoryMapper(messageContext, agentPersonMapper);
        fhirParseService = new FhirParseService();
    }

    private Answer<String> answerWithObjectId() {
        return invocation -> {
            AgentDirectory.AgentKey agentKey = invocation.getArgument(0);
            return String.format("<!--Mocked agentPerson for: %s %s -->",
                agentKey.getPractitionerReference(),
                agentKey.getOrganizationReference());
        };
    }

    @Test
    public void When_MappingAgentDirectory_Expect_CorrectOutputFromMapper() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_AGENT_DIRECTORY);
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        initializeMessageContextWithAgentKeys(bundle);
        var expectedOutput = ResourceTestFileUtils.getFileContent(EXPECTED_AGENT_DIRECTORY);

        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);

        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    @Test
    public void When_MappingAgentDirectoryWithoutPatientManagingOrganizationReference_Expect_Exception() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_AGENT_DIRECTORY_WITHOUT_MANAGING_ORGANIZATION_REFERENCE);
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        initializeMessageContextWithAgentKeys(bundle);

        assertThatThrownBy(() -> agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("The ASR bundle does not contain a Patient resource with the correct identifier and managingOrganization");
    }

    @Test
    public void When_MappingAgentDirectoryWithoutPatientManagingOrganizationResource_Expect_Exception() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_AGENT_DIRECTORY_WITHOUT_MANAGING_ORGANIZATION_RESOURCE);
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        initializeMessageContextWithAgentKeys(bundle);

        assertThatThrownBy(() -> agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("The ASR bundle does not contain a Patient resource with the correct identifier and managingOrganization");
    }

    @Test
    public void When_MappingAgentDirectoryWithPatientManagingOrganizationInAgentKeys_Expect_AgentPersonNotDuplicated() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_AGENT_DIRECTORY);
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        initializeMessageContextWithAgentKeys(bundle);
        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Organization, "5E496953-065B-41F2-9577-BE8F2FBD0757"));

        var expectedOutput = ResourceTestFileUtils.getFileContent(EXPECTED_AGENT_DIRECTORY);

        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);

        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    @Test
    public void When_MappingAgentKeysWithoutAgentKeys_Expect_CorrectOutputFromMapper() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_AGENT_DIRECTORY);
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        messageContext.initialize(bundle);

        var expectedOutput = ResourceTestFileUtils.getFileContent(EXPECTED_AGENT_DIRECTORY_AGENT_PERSON_AS_ORGANIZATION);

        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);

        assertThat(mapperOutput).isEqualTo(expectedOutput);
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

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }
}
