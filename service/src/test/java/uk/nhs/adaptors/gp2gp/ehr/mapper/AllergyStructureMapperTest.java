package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ResourceType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class AllergyStructureMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/allergy/";
    private static final String INPUT_JSON_BUNDLE = TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-1.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-2.json";
    private static final String INPUT_JSON_WITH_PATIENT_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-3.json";
    private static final String INPUT_JSON_WITH_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-4.json";
    private static final String INPUT_JSON_WITH_DATES = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-5.json";
    private static final String INPUT_JSON_WITH_ONSET_DATE_ONLY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-6.json";
    private static final String INPUT_JSON_WITH_REASON_END_DATE_ONLY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-7.json";
    private static final String INPUT_JSON_WITH_NO_DATES = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-8.json";
    private static final String INPUT_JSON_WITH_ENVIRONMENT_CATEGORY = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-9.json";
    private static final String INPUT_JSON_WITH_MEDICATION_CATEGORY = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-10.json";
    private static final String INPUT_JSON_WITH_REACTION = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-11.json";
    private static final String INPUT_JSON_WITH_NO_CATEGORY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-12.json";
    private static final String INPUT_JSON_WITH_UNSUPPORTED_CATEGORY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-13.json";
    private static final String INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_ONE_NOTE = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-14.json";
    private static final String INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_TWO_NOTES = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-15.json";
    private static final String INPUT_JSON_WITH_NO_RELATION_TO_CONDITION = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-16.json";
    private static final String INPUT_JSON_WITH_DEVICE_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-17.json";
    private static final String INPUT_JSON_WITH_RELATED_PERSON_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-18.json";
    private static final String INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NAME_TEXT = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-19.json";
    private static final String INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NO_NAME = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-20.json";
    private static final String INPUT_JSON_WITHOUT_END_DATE = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-without-endDate.json";
    private static final String INPUT_JSON_WITHOUT_ASSERTED_DATE = TEST_FILE_DIRECTORY
            + "example-allergy-intolerance-resource-without-assertedDate.json";
    private static final String INPUT_JSON_WITH_VALID_RECORDER_NO_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-21.json";
    private static final String INPUT_JSON_WITH_INVALID_RECORDER_NO_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-22.json";
    private static final String INPUT_JSON_WITH_VALID_RECORDER_RELATED_PERSON_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-23.json";
    private static final String INPUT_JSON_WITH_VALID_RECORDER_PATIENT_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-24.json";
    private static final String INPUT_JSON_WITH_RESOLVED_CLINICAL_STATUS = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-25.json";


    private static final String OUTPUT_XML_USES_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-1.xml";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-2.xml";
    private static final String OUTPUT_XML_USES_PATIENT_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-3.xml";
    private static final String OUTPUT_XML_USES_DATES = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-4.xml";
    private static final String OUTPUT_XML_USES_ONSET_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-5.xml";
    private static final String OUTPUT_XML_USES_NULL_FLAVOR_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-6.xml";
    private static final String OUTPUT_XML_USES_ENVIRONMENT_CATEGORY = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-7.xml";
    private static final String OUTPUT_XML_USES_MEDICATION_CATEGORY = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-8.xml";
    private static final String OUTPUT_XML_USES_REACTION = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-9.xml";
    private static final String OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_ONE_NOTE = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-10.xml";
    private static final String OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_TWO_NOTES = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-11.xml";
    private static final String OUTPUT_XML_USES_NO_RELATION_TO_CONDITION = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-12.xml";
    private static final String OUTPUT_XML_USES_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-13.xml";
    private static final String OUTPUT_XML_USES_DEVICE_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-14.xml";
    private static final String OUTPUT_XML_USES_RELATED_PERSON_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-15.xml";
    private static final String OUTPUT_XML_USES_RELATED_PERSON_ASSERTER_NO_NAME = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-16.xml";
    private static final String OUTPUT_XML_USES_END_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-17.xml";
    private static final String OUTPUT_XML_USES_NO_END_DATE = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-without-endDate.xml";
    private static final String OUTPUT_XML_USES_NO_ASSERTED_DATE = TEST_FILE_DIRECTORY
            + "expected-output-allergy-structure-without-assertedDate.xml";
    private static final String OUTPUT_XML_USES_NO_AUTHOR_OR_PERFORMER = TEST_FILE_DIRECTORY
            + "expected-output-allergy-structure-18.xml";
    private static final String OUTPUT_XML_USES_RECORDER_AS_PERFORMER_RELATED_PERSON_ASSERTER = TEST_FILE_DIRECTORY
            + "expected-output-allergy-structure-19.xml";
    private static final String OUTPUT_XML_USES_RECORDER_AS_PERFORMER_PATIENT_ASSERTER = TEST_FILE_DIRECTORY
            + "expected-output-allergy-structure-20.xml";
    private static final String OUTPUT_XML_USES_RECORDER_AS_FALLBACK_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-21.xml";
    private static final String OUTPUT_XML_USES_RESOLVED_CLINICAL_STATUS = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-22.xml";
    private static final String COMMON_ID = "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73";

    public static final String CONFIDENTIALITY_CODE = "<confidentialityCode code=\"NOPAT\" "
                      + "codeSystem=\"2.16.840.1.113883.4.642.3.47\" "
                      + "displayName=\"no disclosure to patient, family or caregivers without attending provider's authorization\" />";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private ConfidentialityService confidentialityService;

    private AllergyStructureMapper allergyStructureMapper;
    private MessageContext messageContext;


    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS, OUTPUT_XML_USES_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_NO_OPTIONAL_TEXT_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PATIENT_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_PATIENT_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_DATES, OUTPUT_XML_USES_DATES),
            Arguments.of(INPUT_JSON_WITH_ONSET_DATE_ONLY, OUTPUT_XML_USES_ONSET_DATE),
            Arguments.of(INPUT_JSON_WITH_REASON_END_DATE_ONLY, OUTPUT_XML_USES_END_DATE),
            Arguments.of(INPUT_JSON_WITH_NO_DATES, OUTPUT_XML_USES_NULL_FLAVOR_DATE),
            Arguments.of(INPUT_JSON_WITH_ENVIRONMENT_CATEGORY, OUTPUT_XML_USES_ENVIRONMENT_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_MEDICATION_CATEGORY, OUTPUT_XML_USES_MEDICATION_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_REACTION, OUTPUT_XML_USES_REACTION),
            Arguments.of(INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_ONE_NOTE, OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_ONE_NOTE),
            Arguments.of(INPUT_JSON_WITH_RELATION_TO_CONDITION_WITH_TWO_NOTES, OUTPUT_XML_USES_RELATION_TO_CONDITION_WITH_TWO_NOTES),
            Arguments.of(INPUT_JSON_WITH_NO_RELATION_TO_CONDITION, OUTPUT_XML_USES_NO_RELATION_TO_CONDITION),
            Arguments.of(INPUT_JSON_WITH_DEVICE_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_DEVICE_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RELATED_PERSON_ASSERTER, OUTPUT_XML_USES_RELATED_PERSON_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NAME_TEXT, OUTPUT_XML_USES_RELATED_PERSON_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RELATED_PERSON_ASSERTER_NO_NAME, OUTPUT_XML_USES_RELATED_PERSON_ASSERTER_NO_NAME),
            Arguments.of(INPUT_JSON_WITHOUT_END_DATE, OUTPUT_XML_USES_NO_END_DATE),
            Arguments.of(INPUT_JSON_WITHOUT_ASSERTED_DATE, OUTPUT_XML_USES_NO_ASSERTED_DATE),
            Arguments.of(INPUT_JSON_WITH_VALID_RECORDER_NO_ASSERTER, OUTPUT_XML_USES_RECORDER_AS_FALLBACK_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_INVALID_RECORDER_NO_ASSERTER, OUTPUT_XML_USES_NO_AUTHOR_OR_PERFORMER),
            Arguments.of(INPUT_JSON_WITH_VALID_RECORDER_RELATED_PERSON_ASSERTER,
                    OUTPUT_XML_USES_RECORDER_AS_PERFORMER_RELATED_PERSON_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_VALID_RECORDER_PATIENT_ASSERTER, OUTPUT_XML_USES_RECORDER_AS_PERFORMER_PATIENT_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RESOLVED_CLINICAL_STATUS, OUTPUT_XML_USES_RESOLVED_CLINICAL_STATUS)

        );
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        lenient().when(codeableConceptCdMapper.mapToNullFlavorCodeableConcept(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        lenient().when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        lenient().when(codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(any(CodeableConcept.class),
                any(AllergyIntolerance.AllergyIntoleranceClinicalStatus.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        lenient().when(codeableConceptCdMapper.mapToNullFlavorCodeableConceptForAllergy(any(CodeableConcept.class),
                any(AllergyIntolerance.AllergyIntoleranceClinicalStatus.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        lenient().when(confidentialityService.generateConfidentialityCode(any()))
            .thenReturn(Optional.empty());

        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        List.of(ResourceType.Patient, ResourceType.Device)
            .forEach(resourceType -> messageContext.getIdMapper().getOrNew(resourceType, buildIdType(resourceType, COMMON_ID)));
        List.of(ResourceType.Practitioner, ResourceType.Organization)
            .forEach(resourceType -> messageContext.getAgentDirectory().getAgentId(buildReference(resourceType, COMMON_ID)));
        allergyStructureMapper = new AllergyStructureMapper(
            messageContext,
            codeableConceptCdMapper,
            new ParticipantMapper(),
            confidentialityService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    private static Stream<Arguments> resourceInvalidFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_NO_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_UNSUPPORTED_CATEGORY)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingAllergyIntoleranceJson_Expect_AllergyStructureXmlOutput(String inputJson, String outputXml) {
        final var expectedMessage = ResourceTestFileUtils.getFileContent(outputXml);
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(inputJson);

        String message = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance);
        assertThat(message).contains(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("resourceInvalidFileParams")
    public void When_MappingInvalidAllergyIntoleranceJson_Expect_Exception(String inputJson) {
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(inputJson);

        assertThrows(EhrMapperException.class, ()
            -> allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance));
    }

    @Test
    public void When_ConfidentialityServiceReturnsConfidentialityCode_Expect_MessageContainsConfidentialityCode() {
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS);
        when(confidentialityService.generateConfidentialityCode(allergyIntolerance))
            .thenReturn(Optional.of(CONFIDENTIALITY_CODE));

        final var message = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance);

        assertThat(message).contains(CONFIDENTIALITY_CODE);
    }

    @Test
    public void When_ConfidentialityServiceReturnsEmptyOptional_Expect_MessageDoesNotContainConfidentialityCode() {
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS);

        final var message = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance);

        assertThat(message).doesNotContain(CONFIDENTIALITY_CODE);
    }

    private static AllergyIntolerance parseAllergyIntoleranceFromJsonFile(String filepath) {
        final var jsonInput = ResourceTestFileUtils.getFileContent(filepath);
        return new FhirParseService().parseResource(jsonInput, AllergyIntolerance.class);
    }

}
