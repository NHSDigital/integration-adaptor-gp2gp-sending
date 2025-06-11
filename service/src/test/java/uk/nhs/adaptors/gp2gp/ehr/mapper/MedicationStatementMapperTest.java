package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class MedicationStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/medication_request/";
    private static final String INPUT_JSON_BUNDLE = TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_BUNDLE_WITH_MEDICATION_STATEMENTS = TEST_FILE_DIRECTORY
        + "fhir-bundle-with-medication-statements.json";
    private static final String OUTPUT_XML_WITH_PRESCRIBE_BASED_ON = TEST_FILE_DIRECTORY
        + "medication-statement-with-prescribe-based-on.xml";
    private static final String OUTPUT_XML_WITH_PRESCRIBED_BY_ANOTHER_ORG = TEST_FILE_DIRECTORY
        + "medication-statement-prescribed-by-another-organisation.xml";
    private static final String OUTPUT_XML_NHS_PRESCRIPTION = TEST_FILE_DIRECTORY
        + "medication-statement-nhs-prescription.xml";
    private static final String CONFIDENTIALITY_CODE = """
        <confidentialityCode
            code="NOPAT"
            codeSystem="2.16.840.1.113883.4.642.3.47"
            displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
        />""";

    private static final String PRACTITIONER_RESOURCE_1 = "Practitioner/1";
    private static final String PRACTITIONER_RESOURCE_2 = "Practitioner/2";
    private static final String ORGANIZATION_RESOURCE_1 = "Organization/1";

    @Mock
    private RandomIdGeneratorService mockRandomIdGeneratorService;

    @Mock
    private ConfidentialityService confidentialityService;

    private MessageContext messageContext;
    private CodeableConceptCdMapper codeableConceptCdMapper;
    private MedicationStatementMapper medicationStatementMapper;

    @BeforeEach
    public void setUp() {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        messageContext = new MessageContext(mockRandomIdGeneratorService);
        messageContext.initialize(bundle);
        messageContext.getAgentDirectory().getAgentId(new Reference(buildIdType(ResourceType.Practitioner, "1")));
        messageContext.getAgentDirectory().getAgentId(new Reference(buildIdType(ResourceType.Organization, "2")));
        medicationStatementMapper = new MedicationStatementMapper(
            messageContext,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        assertThatInputMapsToExpectedOutput(TEST_FILE_DIRECTORY + inputJson, TEST_FILE_DIRECTORY + outputXml);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of("mr-with-order-no-optional-fields.json", "medication-statement-with-prescribe-no-optional-fields.xml"),
            Arguments.of(
                "mr-with-order-no-optional-fields-with-medication-codeable-concept.json",
                "medication-statement-with-prescribe-no-optional-fields.xml"
            ),
            Arguments.of("mr-with-order-optional-fields.json", "medication-statement-with-prescribe-optional-fields.xml"),
            Arguments.of("mr-with-complete-status.json", "medication-statement-with-complete-status.xml"),
            Arguments.of("mr-with-active-status.json", "medication-statement-with-active-status.xml"),
            Arguments.of("mr-with-structured-dosage-information.json", "medication-statement-with-active-status.xml"),
            Arguments.of("mr-with-plan-optional-fields.json", "medication-statement-with-authorise-optional-fields.xml"),
            Arguments.of("mr-with-plan-dispense-quantity-text.json", "medication-statement-with-dispense-quantity-text.xml"),
            Arguments.of("mr-with-plan-quantity-quantity-text.json", "medication-statement-with-quantity-quantity-text.xml"),
            Arguments.of("mr-with-plan-no-dispense-quantity-text.json", "medication-statement-with-no-dispense-quantity-text.xml"),
            Arguments.of("mr-with-plan-no-dispense-quantity-value.json", "medication-statement-with-no-dispense-quantity-value.xml"),
            Arguments.of("mr-with-plan-acute-prescription.json", "medication-statement-with-authorise-acute-prescription.xml"),
            Arguments.of("mr-with-plan-repeat-prescription.json", "medication-statement-with-authorise-repeat-prescription.xml"),
            Arguments.of(
                "mr-with-plan-repeat-prescription-no-value.json",
                "medication-statement-with-authorise-repeat-prescription-no-value.xml"
            ),
            Arguments.of("mr-with-plan-start-period-only.json", "medication-statement-with-authorise-start-period-only.xml"),
            Arguments.of("mr-with-plan-no-optional-fields.json", "medication-statement-with-authorise-no-optional-fields.xml"),
            Arguments.of("mr-with-plan-no-status-reason-code.json", "medication-statement-with-authorise-default-status-reason-code.xml"),
            Arguments.of("mr-with-plan-no-info-prescription-text.json", "medication-statement-with-authorise-repeat-prescription.xml"),
            Arguments.of("mr-with-extension-status-reason-with-text.json", "medication-statement-with-status-reason-text.xml"),
            Arguments.of("mr-with-no-recorder-reference.json", "medication-statement-with-no-participant.xml"),
            Arguments.of("mr-with-invalid-recorder-resource-type.json", "medication-statement-with-no-participant.xml"),
            Arguments.of("medication-request-special-character-in-code.json", "medication-statement-with-xml-escaped-text-values.xml"),
            Arguments.of("mr-referencing-medication-with-non-snomed-codes.json", "ms-with-material-coding-containing-translations.xml"),
            Arguments.of("mr-referencing-medication-with-no-snomed-code.json", "ms-with-material-coding-containing-null-flavor-code.xml")
        );
    }

    @SneakyThrows
    private void assertThatInputMapsToExpectedOutput(String inputJsonResourcePath, String outputXmlResourcePath) {
        var expected = ResourceTestFileUtils.getFileContent(outputXmlResourcePath);
        var input = ResourceTestFileUtils.getFileContent(inputJsonResourcePath);
        var parsedMedicationRequest = new FhirParseService().parseResource(input, MedicationRequest.class);

        String outputMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        assertThat(outputMessage).isEqualToNormalizingWhitespace(expected);
    }

    @SneakyThrows
    @Test
    public void When_MappingBasedOnField_Expect_CorrectReferences() {
        var expected = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_PRESCRIBE_BASED_ON);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("123");
        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn("456");


        var inputAuthorise1 = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + "mr-with-plan-acute-prescription.json");
        var parsedMedicationRequest1 = new FhirParseService().parseResource(inputAuthorise1, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest1);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("456", "123", "789");
        var inputWithBasedOn = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + "mr-with-order-based-on.json");
        var parsedMedicationRequestWithBasedOn = new FhirParseService().parseResource(inputWithBasedOn, MedicationRequest.class);
        String outputMessageWithBasedOn =
            medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequestWithBasedOn);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn("789");
        var inputAuthorise2 = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + "mr-with-plan-repeat-prescription.json");
        var parsedMedicationRequest2 = new FhirParseService().parseResource(inputAuthorise2, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest2);

        assertThat(outputMessageWithBasedOn).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("resourceFileExpectException")
    public void When_MappingMedicationRequestWithInvalidResource_Expect_Exception(String inputJson) {
        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);

        assertThrows(EhrMapperException.class, ()
            -> medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest));
    }

    private static List<String> resourceFileExpectException() {
        return List.of(
            "mr-with-no-validity-period.json",
            "mr-with-invalid-intent.json",
            "mr-with-invalid-prescription-type.json",
            "mr-with-invalid-based-on-medication-reference.json",
            "mr-with-invalid-based-on-medication-reference-type.json",
            "mr-with-no-status.json",
            "mr-with-no-dosage-instruction.json",
            "mr-with-no-dispense-request.json",
            "mr-with-order-no-based-on.json",
            "mr-with-plan-status-reason-stopped-no-date.json"
        );
    }

    @Test
    public void When_MappingMedicationRequestWithRequesterWithOnBehalfOf_Expect_ParticipantMappedToAgent() {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        var messageContextMock = mock(MessageContext.class);
        var agentDirectoryMock = mock(AgentDirectory.class);
        var idMapper = new IdMapper(mockRandomIdGeneratorService);
        var medicationRequestIdMapper = new MedicationRequestIdMapper(mockRandomIdGeneratorService);

        when(messageContextMock.getIdMapper()).thenReturn(idMapper);
        when(messageContextMock.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContextMock.getMedicationRequestIdMapper()).thenReturn(medicationRequestIdMapper);
        when(messageContextMock.getAgentDirectory()).thenReturn(agentDirectoryMock);

        medicationStatementMapper = new MedicationStatementMapper(
            messageContextMock,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + "mr-with-requester-on-behalf-of.json");
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        ArgumentCaptor<Reference> agent = ArgumentCaptor.forClass(Reference.class);
        ArgumentCaptor<Reference> onBehalfOf = ArgumentCaptor.forClass(Reference.class);
        verify(agentDirectoryMock).getAgentRef(agent.capture(), onBehalfOf.capture());

        assertThat(agent.getValue().getReference()).isEqualTo(PRACTITIONER_RESOURCE_1);
        assertThat(onBehalfOf.getValue().getReference()).isEqualTo(ORGANIZATION_RESOURCE_1);
    }

    private static Stream<Arguments> resourceFilesWithParticipant() {
        return Stream.of(
            Arguments.of("mr-with-requester.json", PRACTITIONER_RESOURCE_1),
            Arguments.of("mr-with-no-requester.json", PRACTITIONER_RESOURCE_2),
            Arguments.of("mr-with-requester-agent-as-org.json", ORGANIZATION_RESOURCE_1),
            Arguments.of("mr-with-requester-org-and-on-behalf-of.json", ORGANIZATION_RESOURCE_1)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceFilesWithParticipant")
    public void When_MappingMedicationRequestWithParticipant_Expect_ParticipantMappedToAgent(
        String inputJson, String agentId) {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        var messageContextMock = mock(MessageContext.class);
        var agentDirectoryMock = mock(AgentDirectory.class);
        var idMapper = new IdMapper(mockRandomIdGeneratorService);
        var medicationRequestIdMapper = new MedicationRequestIdMapper(mockRandomIdGeneratorService);

        when(messageContextMock.getIdMapper()).thenReturn(idMapper);
        when(messageContextMock.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContextMock.getMedicationRequestIdMapper()).thenReturn(medicationRequestIdMapper);
        when(messageContextMock.getAgentDirectory()).thenReturn(agentDirectoryMock);

        medicationStatementMapper = new MedicationStatementMapper(
            messageContextMock,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);
        medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        ArgumentCaptor<Reference> agent = ArgumentCaptor.forClass(Reference.class);
        verify(agentDirectoryMock).getAgentId(agent.capture());

        assertThat(agent.getValue().getReference()).isEqualTo(agentId);
    }

    private static Stream<Arguments> resourceFilesWithMedicationStatement() {
        return Stream.of(
            Arguments.of("mr-prescribed-by-another-organisation.json", OUTPUT_XML_WITH_PRESCRIBED_BY_ANOTHER_ORG),
            Arguments.of("mr-prescribed-by-gp-practice.json", OUTPUT_XML_NHS_PRESCRIPTION),
            Arguments.of("mr-prescribed-by-previous-practice.json", OUTPUT_XML_NHS_PRESCRIPTION),
            Arguments.of("mr-empty-prescribing-agency-coding-array.json", OUTPUT_XML_NHS_PRESCRIPTION),
            Arguments.of("mr-missing-prescribing-agency-codeable-concept.json", OUTPUT_XML_NHS_PRESCRIPTION)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceFilesWithMedicationStatement")
    public void When_MappingMedicationRequest_WithMedicationStatement_Expect_PrescribingAgencyMappedToSupplyType(
        String inputJson, String outputXml) {

        var expected = ResourceTestFileUtils.getFileContent(outputXml);

        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE_WITH_MEDICATION_STATEMENTS);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        var messageContextMock = mock(MessageContext.class);
        var agentDirectoryMock = mock(AgentDirectory.class);
        var idMapper = new IdMapper(mockRandomIdGeneratorService);
        var medicationRequestIdMapper = new MedicationRequestIdMapper(mockRandomIdGeneratorService);

        when(messageContextMock.getIdMapper()).thenReturn(idMapper);
        when(messageContextMock.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContextMock.getMedicationRequestIdMapper()).thenReturn(medicationRequestIdMapper);
        when(messageContextMock.getAgentDirectory()).thenReturn(agentDirectoryMock);

        medicationStatementMapper = new MedicationStatementMapper(
            messageContextMock,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            mockRandomIdGeneratorService,
            confidentialityService
        );

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);
        var outputString = medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        assertXmlIsEqual(outputString, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "mr-with-plan-no-optional-fields.json",
        "mr-with-order-no-optional-fields.json"
    })
    public void When_ConfidentialityServiceReturnsConfidentialityCode_Expect_MessageContainsConfidentialityCode(
        String inputJson
    ) {
        final var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputJson);
        final var parsedMedicationRequest = new FhirParseService()
            .parseResource(jsonInput, MedicationRequest.class);
        when(confidentialityService.generateConfidentialityCode(parsedMedicationRequest))
            .thenReturn(Optional.of(CONFIDENTIALITY_CODE));

        final var actualMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(
            parsedMedicationRequest
        );

        assertThat(actualMessage)
            .contains(CONFIDENTIALITY_CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "mr-with-plan-no-optional-fields.json",
        "mr-with-order-no-optional-fields.json"
    })
    public void When_ConfidentialityServiceReturnsEmptyOptional_Expect_MessageDoesNotContainConfidentialityCode(
        String inputJson
    ) {
        final var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputJson);
        final var parsedMedicationRequest = new FhirParseService()
            .parseResource(jsonInput, MedicationRequest.class);
        when(confidentialityService.generateConfidentialityCode(parsedMedicationRequest))
            .thenReturn(Optional.empty());

        final var actualMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(
            parsedMedicationRequest
        );

        assertThat(actualMessage)
            .doesNotContain(CONFIDENTIALITY_CODE);
    }

    @Test
    void When_MedicationRequestPlanHasInvalidPrescriptionType_Expect_EhrMapperExceptionThrown() {
        final var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + "mr-plan-with-invalid-prescription-type.json");
        final var parsedMedicationRequest = new FhirParseService()
            .parseResource(jsonInput, MedicationRequest.class);

        var exception = assertThrows(
            EhrMapperException.class,
            () -> medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest));

        assertThat(exception.getMessage()).isEqualTo("Could not resolve Prescription Type of `invalid-type` in MedicationRequest/789");
    }

    @Test
    void When_MedicationRequestPlanHasUnrecognisedUncodedPrescriptionType_Expect_EhrMapperExceptionThrown() {
        final var jsonInput = ResourceTestFileUtils.getFileContent(
            TEST_FILE_DIRECTORY + "mr-plan-with-unrecognised-uncoded-prescription-type.json"
        );
        final var parsedMedicationRequest = new FhirParseService()
            .parseResource(jsonInput, MedicationRequest.class);

        var exception = assertThrows(
            EhrMapperException.class,
            () -> medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest));

        assertThat(exception.getMessage())
            .isEqualTo("Could not resolve Prescription Type with text of `Some medical information` in MedicationRequest/789");

    }

    private void assertXmlIsEqual(String outputString, String expected) {

        Diff diff = DiffBuilder.compare(outputString).withTest(expected)
            .checkForIdentical()
            .ignoreWhitespace()
            .build();

        assertThat(diff.hasDifferences())
            .as("Xml is not equal: " + System.lineSeparator() + diff.fullDescription())
            .isFalse();
    }
}
