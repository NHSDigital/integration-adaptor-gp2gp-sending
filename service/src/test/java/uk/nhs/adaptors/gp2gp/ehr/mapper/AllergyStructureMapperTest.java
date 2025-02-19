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

import org.springframework.util.StringUtils;
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
    private static final String INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "input-with-optional-text-fields.json";

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
            Arguments.of("input-with-optional-text-fields.json", "expected-uses-optional-text-fields.xml"),
            Arguments.of("input-with-no-optional-text-fields.json", "expected-uses-no-optional-text-fields.xml"),
            Arguments.of("input-with-patient-recorder-and-asserter.json", "expected-uses-patient-recorder-and-asserter.xml"),
            Arguments.of("input-with-recorder-and-asserter.json", "expected-uses-recorder-and-asserter.xml"),
            Arguments.of("input-with-dates.json", "expected-uses-dates.xml"),
            Arguments.of("input-with-onset-date-only.json", "expected-uses-onset-date.xml"),
            Arguments.of("input-with-reason-end-date-only.json", "expected-uses-end-date.xml"),
            Arguments.of("input-with-no-dates.json", "expected-uses-null-flavor-date.xml"),
            Arguments.of("input-with-environment-category.json", "expected-uses-environment-category.xml"),
            Arguments.of("input-with-medication-category.json", "expected-uses-medication-category.xml"),
            Arguments.of("input-with-reaction.json", "expected-uses-reaction.xml"),
            Arguments.of("input-with-relation-to-condition-with-one-note.json", "expected-uses-relation-to-condition-with-one-note.xml"),
            Arguments.of("input-with-relation-to-condition-with-two-notes.json", "expected-uses-relation-to-condition-with-two-notes.xml"),
            Arguments.of("input-with-no-relation-to-condition.json", "expected-uses-no-relation-to-condition.xml"),
            Arguments.of("input-with-device-recorder-and-asserter.json", "expected-uses-device-recorder-and-asserter.xml"),
            Arguments.of("input-with-related-person-asserter.json", "expected-uses-related-person-asserter.xml"),
            Arguments.of("input-with-related-person-asserter-name-text.json", "expected-uses-related-person-asserter.xml"),
            Arguments.of("input-with-related-person-asserter-no-name.json", "expected-uses-related-person-asserter-no-name.xml"),
            Arguments.of("input-without-endDate.json", "expected-without-endDate.xml"),
            Arguments.of("input-without-assertedDate.json", "expected-without-assertedDate.xml"),
            Arguments.of("input-with-valid-recorder-no-asserter.json", "expected-uses-recorder-as-fallback-asserter.xml"),
            Arguments.of("input-with-invalid-recorder-no-asserter.json", "expected-uses-no-author-or-performer.xml"),
            Arguments.of(
                "input-with-valid-recorder-related-person-asserter.json",
                "expected-uses-recorder-as-performer-related-person-asserter.xml"
            ),
            Arguments.of("input-with-valid-recorder-patient-asserter.json", "expected-uses-recorder-as-performer-patient-asserter.xml"),
            Arguments.of("input-with-resolved-clinical-status.json", "expected-uses-resolved-clinical-status.xml")
        );
    }

    @BeforeEach
    void setUp() {
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
    void tearDown() {
        messageContext.resetMessageContext();
    }

    private static Stream<Arguments> resourceInvalidFileParams() {
        return Stream.of(
            Arguments.of("input-with-no-category.json"),
            Arguments.of("input-with-unsupported-category.json")
        );
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    void When_MappingAllergyIntoleranceJson_Expect_AllergyStructureXmlOutput(String inputJson, String outputXml) {
        final var expectedMessage = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + outputXml);
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(TEST_FILE_DIRECTORY + inputJson);

        String message = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance);
        assertThat(message).contains(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("resourceInvalidFileParams")
    void When_MappingInvalidAllergyIntoleranceJson_Expect_Exception(String inputJson) {
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(TEST_FILE_DIRECTORY + inputJson);

        assertThrows(EhrMapperException.class, ()
            -> allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance));
    }

    @Test
    void When_ConfidentialityServiceReturnsConfidentialityCode_Expect_MessageContainsConfidentialityCode() {
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS);
        when(confidentialityService.generateConfidentialityCode(allergyIntolerance))
            .thenReturn(Optional.of(CONFIDENTIALITY_CODE));

        final var message = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance);

        assertThat(message).contains(CONFIDENTIALITY_CODE);
        assertThat(StringUtils.countOccurrencesOf(message, CONFIDENTIALITY_CODE))
            .withFailMessage("<confidentialityCode /> should appear within both the CompoundStatement and ObservationStatement")
            .isEqualTo(2);
    }

    @Test
    void When_ConfidentialityServiceReturnsEmptyOptional_Expect_MessageDoesNotContainConfidentialityCode() {
        final var allergyIntolerance = parseAllergyIntoleranceFromJsonFile(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS);

        final var message = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(allergyIntolerance);

        assertThat(message).doesNotContain(CONFIDENTIALITY_CODE);
    }

    private static AllergyIntolerance parseAllergyIntoleranceFromJsonFile(String filepath) {
        final var jsonInput = ResourceTestFileUtils.getFileContent(filepath);
        return new FhirParseService().parseResource(jsonInput, AllergyIntolerance.class);
    }

}
