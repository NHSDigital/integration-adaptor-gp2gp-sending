package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.FileParsingUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil.ACTUAL_PROBLEM_CODE;
import static uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE;
import static uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil.TEST_CONDITION_CODE;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOSCRUB;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;
import static uk.nhs.adaptors.gp2gp.utils.XmlAssertion.assertThatXml;
import static uk.nhs.adaptors.gp2gp.utils.XmlParsingUtility.wrapXmlInRootElement;

@ExtendWith(MockitoExtension.class)
class ConditionLinkSetMapperTest {

    private static final String CONDITION_ID = "7E277DF1-6F1C-47CD-84F7-E9B7BF4105DB-PROB";
    private static final String GENERATED_ID = "50233a2f-128f-4b96-bdae-6207ed11a8ea";
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/condition/";
    private static final String INPUT_JSON_BUNDLE = "fhir-bundle.json";

    @Mock
    private IdMapper idMapper;
    @Mock
    private AgentDirectory agentDirectory;
    @Mock
    private MessageContext messageContext;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private ConfidentialityService confidentialityService;
    @Captor
    private ArgumentCaptor<Condition> conditionArgumentCaptor;

    private ConditionLinkSetMapper conditionLinkSetMapper;

    @BeforeEach
    void setUp() {
        var bundleInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + INPUT_JSON_BUNDLE);
        var participantMapper = new ParticipantMapper();
        final Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        final IdType conditionId = buildIdType(ResourceType.Condition, CONDITION_ID);
        final InputBundle inputBundle = new InputBundle(bundle);

        when(messageContext.getIdMapper())
            .thenReturn(idMapper);
        when(messageContext.getInputBundleHolder())
            .thenReturn(inputBundle);
        when(idMapper.getOrNew(ResourceType.Condition, conditionId))
            .thenReturn(CONDITION_ID);
        when(idMapper.getOrNew(any(Reference.class)))
            .thenAnswer(answerWithId());

        conditionLinkSetMapper = new ConditionLinkSetMapper(
            messageContext,
            randomIdGeneratorService,
            codeableConceptCdMapper,
            participantMapper,
            confidentialityService
        );
    }

    @AfterEach
    void afterEach() {
        messageContext.resetMessageContext();
    }

    private void initializeNullFlavorCodeableConceptMocks() {
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(NULL_FLAVOR_CODE);
    }

    private void initializeAgentDirectoryMocks() {
        when(messageContext.getAgentDirectory())
            .thenReturn(agentDirectory);
    }

    static Stream<Arguments> testArguments() {
        return Stream.of(
            Arguments.of("condition_all_included.json", "expected_output_linkset_1.xml", true),
            Arguments.of("condition_all_included.json", "expected_output_linkset_2.xml", false),
            Arguments.of("condition_major_significance.json", "expected_output_linkset_6.xml", false),
            Arguments.of("condition_related_clinical_content_list_reference.json", "expected_output_linkset_8.xml", false),
            Arguments.of("condition_related_clinical_content_suppressed_linkage_references.json", "expected_output_linkset_9.xml", false),
            Arguments.of("condition_2_related_clinical_content.json", "expected_output_linkset_10.xml", false),
            Arguments.of("condition_status_active.json", "expected_output_linkset_11.xml", false),
            Arguments.of("condition_status_inactive.json", "expected_output_linkset_12.xml", false),
            Arguments.of("condition_dates_present.json", "expected_output_linkset_13.xml", false),
            Arguments.of("condition_dates_not_present.json", "expected_output_linkset_14.xml", false)
        );
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    void When_MappingParsedCondition_With_RealProblem_Expect_LinkSetXml(String conditionJson, String outputXml, boolean isNested) {
        final Condition condition = getConditionResourceFromJson(conditionJson);
        final String expectedXml = getXmlStringFromFile(outputXml);

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    private static Stream<Arguments> testObservationArguments() {
        return Stream.of(
            Arguments.of("condition_no_problem.json", "expected_output_linkset_3.xml", true),
            Arguments.of("condition_actual_problem_condition.json", "expected_output_linkset_5.xml", false),
            Arguments.of("condition_no_problem_no_onsetdate.json", "expected_output_linkset_18.xml", true),
            Arguments.of("condition_related_clinical_content_allergy.json", "expected_output_linkset_19.xml", false)
        );
    }

    @ParameterizedTest
    @MethodSource("testObservationArguments")
    void When_MappingParsedCondition_With_ObservationActualProblem_Expect_LinkSetXml(
        String conditionJson,
        String outputXml,
        boolean isNested
    ) {
        final Condition condition = getConditionResourceFromJson(conditionJson);
        final String expectedXml = getXmlStringFromFile(outputXml);

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());
        when(randomIdGeneratorService.createNewId())
            .thenReturn(GENERATED_ID);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingParsedConditionWithoutMappedAgent_Expect_EhrMapperException() {
        final EhrMapperException propagatedException = new EhrMapperException("expected exception");
        final Condition condition = getConditionResourceFromJson("condition_status_active.json");

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenThrow(propagatedException);

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(condition, false))
            .isEqualTo(propagatedException);
    }

    @Test
    void When_MappingParsedConditionWithAsserterNotPractitioner_Expect_EhrMapperException() {
        final Condition condition = getConditionResourceFromJson("condition_asserter_not_practitioner.json");

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(condition, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Condition.asserter must be a Practitioner");
    }

    @Test
    void When_MappingParsedCondition_With_ActualProblemImmunization_Expect_LinkSetXml() {
        final Condition condition = getConditionResourceFromJson("condition_actual_problem_immunization.json");
        final String expectedXml = getXmlStringFromFile("expected_output_linkset_17.xml");

        initializeAgentDirectoryMocks();
        when(codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(any(CodeableConcept.class)))
            .thenReturn(TEST_CONDITION_CODE);
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());
        when(randomIdGeneratorService.createNewId())
            .thenReturn(GENERATED_ID);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingParsedCondition_With_ActualProblemMedicationRequest_Expect_LinkSetXml() {
        final Condition condition = getConditionResourceFromJson("condition_actual_problem_medication_request.json");
        final String expectedXml = getXmlStringFromFile("expected_output_linkset_20.xml");

        initializeAgentDirectoryMocks();
        when(codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(any(CodeableConcept.class)))
            .thenReturn(ACTUAL_PROBLEM_CODE);
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());
        when(randomIdGeneratorService.createNewId())
            .thenReturn(GENERATED_ID);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingParsedCondition_With_AllergyIntolerance_Expect_LinkSetXml() {
        final Condition condition = getConditionResourceFromJson("condition_actual_problem_allergy_intolerance.json");
        final String expectedXml = getXmlStringFromFile("expected_output_linkset_16.xml");

        initializeAgentDirectoryMocks();
        when(codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(any(CodeableConcept.class)))
            .thenReturn(TEST_CONDITION_CODE);
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());
        when(randomIdGeneratorService.createNewId())
                .thenReturn(GENERATED_ID);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingParsedConditionCodeIsMissing_Expect_MapperException() {
        final Condition condition = getConditionResourceFromJson("condition_missing_code.json");

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(condition, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Condition code not present");
    }

    @Test
    void When_MappingParsedConditionWithNoRelatedClinicalContent_Expect_LinkSetXml() {
        final Condition condition = getConditionResourceFromJson("condition_no_related_clinical_content.json");
        final String expectedXml = getXmlStringFromFile("expected_output_linkset_8.xml");

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingConditionWithSuppressedMedicationRequestAsRelatedClinicalContent_Expect_NoEntry() {
        final Condition condition = getConditionResourceFromJson("condition_suppressed_related_medication_request.json");
        final String expectedXml = getXmlStringFromFile("expected_output_linkset_15.xml");

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenAnswer(answerWithId());

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(expectedXml).isEqualTo(actualXml);
    }


    @Test
    void When_MappingParsedCondition_With_AsserterNotPresent_Expect_LinkSetXml() {
        final Condition condition = getConditionResourceFromJson("condition_asserter_not_present.json");
        final String expectedXml = getXmlStringFromFile("expected_output_linkset_21.xml");

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingCondition_With_NopatMetaSecurity_Expect_ConfidentialityCodeInBothLinksetAndObservationStatement() {
        final Condition condition = getConditionResourceFromJson("condition_actual_problem_condition.json");
        final String linkSetXpath = "/Root/component[1]/LinkSet/" + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();
        final String observationStatementXpath = "/Root/component[2]/ObservationStatement/" + ConfidentialityCodeUtility
            .getNopatConfidentialityCodeXpathSegment();
        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(condition);

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(confidentialityService.generateConfidentialityCode(conditionArgumentCaptor.capture()))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final String actualXml = wrapXmlInRootElement(conditionLinkSetMapper.mapConditionToLinkSet(condition, false));
        final String conditionSecurityCode = ConfidentialityCodeUtility.getSecurityCodeFromResource(conditionArgumentCaptor.getValue());

        assertAll(
            () -> assertThatXml(actualXml).containsXPath(observationStatementXpath),
            () -> assertThatXml(actualXml).containsXPath(linkSetXpath),
            () -> assertThat(conditionSecurityCode).isEqualTo(NOPAT)
        );
    }

    @Test
    void When_MappingCondition_With_NoScrubMetaSecurity_Expect_ConfidentialityCodeNotPresent() {
        final Condition condition = getConditionResourceFromJson("condition_suppressed_related_medication_request.json");
        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(condition);

        initializeAgentDirectoryMocks();
        initializeNullFlavorCodeableConceptMocks();
        when(confidentialityService.generateConfidentialityCode(conditionArgumentCaptor.capture()))
            .thenReturn(Optional.empty());

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);
        final String conditionSecurityCode = ConfidentialityCodeUtility.getSecurityCodeFromResource(conditionArgumentCaptor.getValue());

        assertAll(
            () -> assertThat(actualXml).doesNotContainIgnoringCase(NOPAT_HL7_CONFIDENTIALITY_CODE),
            () -> assertThat(conditionSecurityCode).isEqualTo(NOSCRUB)
        );
    }

    private Answer<String> answerWithId() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return reference.getReferenceElement().getIdPart();
        };
    }

    private Condition getConditionResourceFromJson(String filename) {
        return FileParsingUtility.parseResourceFromJsonFile(TEST_FILES_DIRECTORY + filename, Condition.class);
    }

    private String getXmlStringFromFile(String filename) {
        return ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + filename);
    }
}