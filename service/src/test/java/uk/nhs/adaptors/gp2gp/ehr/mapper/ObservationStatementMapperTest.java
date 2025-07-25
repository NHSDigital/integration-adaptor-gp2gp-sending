package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext.REDACTION_INTERACTION_ID;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class ObservationStatementMapperTest {
    private static final String TEST_ID = "394559384658936";

    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/observation/";

    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY
        + "example-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NOPAT = TEST_FILE_DIRECTORY + "example-observation-resource-with-nopat.json";
    private static final String INPUT_JSON_WITH_NO_PERTINENT_INFORMATION_DATA = TEST_FILE_DIRECTORY
            + "example-observation-resource-with-no-pertinent-information-data.json";
    private static final String INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY
        + "example-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = TEST_FILE_DIRECTORY
        + "example-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = TEST_FILE_DIRECTORY
        + "example-observation-resource-4.json";
    private static final String INPUT_JSON_WITH_STRING_VALUE = TEST_FILE_DIRECTORY
        + "example-observation-resource-6.json";
    private static final String INPUT_JSON_WITH_QUANTITY_VALUE = TEST_FILE_DIRECTORY
        + "example-observation-resource-7.json";
    private static final String INPUT_JSON_WITH_CODEABLE_CONCEPT_VALUE = TEST_FILE_DIRECTORY
        + "example-observation-resource-8.json";
    private static final String INPUT_JSON_WITH_SAMPLED_DATA_VALUE = TEST_FILE_DIRECTORY
        + "example-observation-resource-9.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE_AND_QUANTITY = TEST_FILE_DIRECTORY
        + "example-observation-resource-10.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE = TEST_FILE_DIRECTORY
        + "example-observation-resource-11.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_HIGH_1 = TEST_FILE_DIRECTORY
        + "example-observation-resource-12.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_INVALID_CODE = TEST_FILE_DIRECTORY
        + "example-observation-resource-13.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_INVALID_SYSTEM = TEST_FILE_DIRECTORY
        + "example-observation-resource-14.json";
    private static final String INPUT_JSON_WITH_TWO_INTERPRETATION_USER_SELECTED = TEST_FILE_DIRECTORY
        + "example-observation-resource-15.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_INTERPRETATIONS = TEST_FILE_DIRECTORY
        + "example-observation-resource-16.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_HIGH_2 = TEST_FILE_DIRECTORY
        + "example-observation-resource-17.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_HIGH_3 = TEST_FILE_DIRECTORY
        + "example-observation-resource-18.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_LOW_1 = TEST_FILE_DIRECTORY
        + "example-observation-resource-19.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_LOW_2 = TEST_FILE_DIRECTORY
        + "example-observation-resource-20.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_LOW_3 = TEST_FILE_DIRECTORY
        + "example-observation-resource-21.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_ABNORMAL_1 = TEST_FILE_DIRECTORY
        + "example-observation-resource-22.json";
    private static final String INPUT_JSON_WITH_INTERPRETATION_ABNORMAL_2 = TEST_FILE_DIRECTORY
        + "example-observation-resource-23.json";
    private static final String INPUT_JSON_WITH_PARTICIPANT = TEST_FILE_DIRECTORY
        + "example-observation-resource-24.json";
    private static final String INPUT_JSON_WITH_COMPONENT_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "example-observation-resource-26.json";
    private static final String INPUT_JSON_WITH_COMPONENT_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "example-observation-resource-27.json";
    private static final String INPUT_JSON_WITH_COMPONENT_VALUE_STRING = TEST_FILE_DIRECTORY
        + "example-observation-resource-28.json";
    private static final String INPUT_JSON_WITH_COMPONENT_VALUE_OTHER_TYPES = TEST_FILE_DIRECTORY
        + "example-observation-resource-29.json";
    private static final String INPUT_JSON_WITH_COMPONENT_USER_SELECTED_INTERPRETATION = TEST_FILE_DIRECTORY
        + "example-observation-resource-30.json";
    private static final String INPUT_JSON_WITH_COMPONENT_FIRST_INTERPRETATION = TEST_FILE_DIRECTORY
        + "example-observation-resource-31.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_COMPONENTS = TEST_FILE_DIRECTORY
        + "example-observation-resource-32.json";
    private static final String INPUT_JSON_WITH_NO_COMPONENT = TEST_FILE_DIRECTORY
        + "example-observation-resource-33.json";
    private static final String INPUT_JSON_WITH_PARTICIPANT_INVALID_REFERENCE_RESOURCE_TYPE = TEST_FILE_DIRECTORY
        + "example-observation-resource-34.json";
    private static final String INPUT_JSON_WITH_ATTACHMENT_VALUE = TEST_FILE_DIRECTORY
        + "example-observation-resource-35.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_START = TEST_FILE_DIRECTORY
        + "example-observation-resource-36.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_BLANK = TEST_FILE_DIRECTORY
        + "example-observation-resource-37.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE_NO_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "example-observation-resource-41.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE_NO_LOW_NO_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "example-observation-resource-42.json";
    private static final String INPUT_JSON_WITH_REFERENCE_RANGE_NO_HIGH_NO_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "example-observation-resource-43.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_END = TEST_FILE_DIRECTORY
        + "example-observation-resource-44.json";
    private static final String INPUT_JSON_WITH_BODY_SITE = TEST_FILE_DIRECTORY
        + "example-observation-with-body-site.json";
    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-1.xml";
    private static final String OUTPUT_XML_USES_UNK_DATE_TIME = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-2.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-3.xml";
    private static final String OUTPUT_XML_WITH_NOPAT = TEST_FILE_DIRECTORY
        + "expected-observation-statement-with-confidentiality-code.xml";
    private static final String OUTPUT_XML_WITH_NO_PERTINENT_INFORMATION = TEST_FILE_DIRECTORY
            + "expected-observation-statement-without-pertinent-information.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-4.xml";
    private static final String OUTPUT_XML_WITH_STRING_VALUE = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-5.xml";
    private static final String OUTPUT_XML_WITH_QUANTITY_VALUE = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-6.xml";
    private static final String OUTPUT_XML_WITH_CODEABLE_CONCEPT_VALUE = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-7.xml";
    private static final String OUTPUT_XML_WITH_REFERENCE_RANGE_AND_QUANTITY = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-9.xml";
    private static final String OUTPUT_XML_WITH_REFERENCE_RANGE = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-10.xml";
    private static final String OUTPUT_XML_WITH_INTERPRETATION_CODE = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-11.xml";
    private static final String OUTPUT_XML_WITH_INTERPRETATION_CODE_INVALID_CODE = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-12.xml";
    private static final String OUTPUT_XML_WITH_INTERPRETATION_CODE_INVALID_SYSTEM = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-13.xml";
    private static final String OUTPUT_XML_WITH_TWO_INTERPRETATION_USER_SELECTED = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-14.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_INTERPRETATIONS = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-15.xml";
    private static final String OUTPUT_XML_WITH_INTERPRETATION_CODE_LOW = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-16.xml";
    private static final String OUTPUT_XML_WITH_INTERPRETATION_CODE_ABNORMAL = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-17.xml";
    private static final String OUTPUT_XML_WITH_PARTICIPANT = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-18.xml";
    private static final String OUTPUT_XML_WITH_COMPONENT_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-19.xml";
    private static final String OUTPUT_XML_WITH_COMPONENT_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-20.xml";
    private static final String OUTPUT_XML_WITH_COMPONENT_VALUE_STRING = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-21.xml";
    private static final String OUTPUT_XML_WITH_COMPONENT_VALUE_OTHER_TYPES = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-22.xml";
    private static final String OUTPUT_XML_WITH_COMPONENT_USER_SELECTED_INTERPRETATION = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-23.xml";
    private static final String OUTPUT_XML_WITH_COMPONENT_FIRST_INTERPRETATION = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-24.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_COMPONENTS = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-25.xml";
    private static final String OUTPUT_XML_WITH_NO_COMPONENT = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-26.xml";
    private static final String OUTPUT_XML_WITH_EFFECTIVE_TIME_UNK_LOW = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-27.xml";
    private static final String OUTPUT_XML_WITH_EFFECTIVE_TIME_UNK = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-28.xml";
    private static final String OUTPUT_XML_WITH_REFERENCE_RANGE_NO_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-29.xml";
    private static final String OUTPUT_XML_WITH_REFERENCE_RANGE_NO_LOW_NO_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-30.xml";
    private static final String OUTPUT_XML_WITH_REFERENCE_RANGE_NO_HIGH_NO_VALUE_QUANTITY = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-31.xml";
    private static final String OUTPUT_XML_WITH_EFFECTIVE_TIME_NO_END = TEST_FILE_DIRECTORY
        + "expected-output-observation-statement-32.xml";
    private static final String OUTPUT_XML_WITH_BODY_SITE = TEST_FILE_DIRECTORY
            + "expected-output-observation-with-body-site.xml";

    private static final String CONFIDENTIALITY_CODE =
        "<confidentialityCode code=\"NOPAT\" codeSystem=\"2.16.840.1.113883.4.642.3.47\" "
        + "displayName=\"no disclosure to patient, family or caregivers without attending provider's authorization\" />";

    private CharSequence expectedOutputMessage;
    private ObservationStatementMapper observationStatementMapper;
    private MessageContext messageContext;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    @BeforeEach
    public void setUp() {
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        lenient().when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(new Bundle());
        observationStatementMapper = new ObservationStatementMapper(messageContext,
            new StructuredObservationValueMapper(),
            new PertinentInformationObservationValueMapper(),
            codeableConceptCdMapper,
            new ParticipantMapper(),
            new ConfidentialityService(new RedactionsContext(REDACTION_INTERACTION_ID)));
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    void When_NoPertinentInformation_Expect_ContentSuppressed() {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_NO_PERTINENT_INFORMATION);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_PERTINENT_INFORMATION_DATA);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @Test
    void When_MappingParsedObservationWithNopat_Expect_ObservationStatementWithConfidentialityCode() {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_NOPAT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NOPAT);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true);

        assertThat(outputMessage).contains(CONFIDENTIALITY_CODE);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_ObservationStatementXmlOutput(String inputJson, String outputXml) {
        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Practitioner, "something"));

        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, false);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsThrowError")
    public void When_MappingObservationJson_Expect_ErrorThrown(String inputJson, Class expectedClass) {
        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Practitioner, "something"));

        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        var exception = assertThrows(EhrMapperException.class, () ->
            observationStatementMapper.mapObservationToObservationStatement(parsedObservation, false));

        assertThat(exception.getMessage()).isEqualTo(String.format(
            "Observation value type %s not supported.", expectedClass));
    }

    @Test
    public void When_MappingParsedObservationJsonWithNestedTrue_Expect_ObservationStatementXmlOutput() {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @Test
    public void When_MappingObservationWithInvalidParticipantResourceType_Expect_Exception() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PARTICIPANT_INVALID_REFERENCE_RESOURCE_TYPE);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThatThrownBy(() -> observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Invalid performer reference in observation resource with id: "
                    + "Consultation3-Topic2-Category-Examination-Observation-1");
    }

    private static Stream<Arguments> resourceFileParamsThrowError() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_SAMPLED_DATA_VALUE, SampledData.class),
            Arguments.of(INPUT_JSON_WITH_ATTACHMENT_VALUE, Attachment.class)
        );
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_EFFECTIVE_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_UNK_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD, OUTPUT_XML_USES_EFFECTIVE_PERIOD),
            Arguments.of(INPUT_JSON_WITH_ISSUED_ONLY, OUTPUT_XML_USES_UNK_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_STRING_VALUE, OUTPUT_XML_WITH_STRING_VALUE),
            Arguments.of(INPUT_JSON_WITH_QUANTITY_VALUE, OUTPUT_XML_WITH_QUANTITY_VALUE),
            Arguments.of(INPUT_JSON_WITH_CODEABLE_CONCEPT_VALUE, OUTPUT_XML_WITH_CODEABLE_CONCEPT_VALUE),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE_AND_QUANTITY, OUTPUT_XML_WITH_REFERENCE_RANGE_AND_QUANTITY),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE, OUTPUT_XML_WITH_REFERENCE_RANGE),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_HIGH_1, OUTPUT_XML_WITH_INTERPRETATION_CODE),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_INVALID_CODE, OUTPUT_XML_WITH_INTERPRETATION_CODE_INVALID_CODE),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_INVALID_SYSTEM, OUTPUT_XML_WITH_INTERPRETATION_CODE_INVALID_SYSTEM),
            Arguments.of(INPUT_JSON_WITH_TWO_INTERPRETATION_USER_SELECTED, OUTPUT_XML_WITH_TWO_INTERPRETATION_USER_SELECTED),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_INTERPRETATIONS, OUTPUT_XML_WITH_MULTIPLE_INTERPRETATIONS),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_HIGH_2, OUTPUT_XML_WITH_INTERPRETATION_CODE),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_HIGH_3, OUTPUT_XML_WITH_INTERPRETATION_CODE),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_LOW_1, OUTPUT_XML_WITH_INTERPRETATION_CODE_LOW),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_LOW_2, OUTPUT_XML_WITH_INTERPRETATION_CODE_LOW),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_LOW_3, OUTPUT_XML_WITH_INTERPRETATION_CODE_LOW),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_ABNORMAL_1, OUTPUT_XML_WITH_INTERPRETATION_CODE_ABNORMAL),
            Arguments.of(INPUT_JSON_WITH_INTERPRETATION_ABNORMAL_2, OUTPUT_XML_WITH_INTERPRETATION_CODE_ABNORMAL),
            Arguments.of(INPUT_JSON_WITH_PARTICIPANT, OUTPUT_XML_WITH_PARTICIPANT),
            Arguments.of(INPUT_JSON_WITH_COMPONENT_OPTIONAL_FIELDS, OUTPUT_XML_WITH_COMPONENT_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_COMPONENT_VALUE_QUANTITY, OUTPUT_XML_WITH_COMPONENT_VALUE_QUANTITY),
            Arguments.of(INPUT_JSON_WITH_COMPONENT_VALUE_STRING, OUTPUT_XML_WITH_COMPONENT_VALUE_STRING),
            Arguments.of(INPUT_JSON_WITH_COMPONENT_VALUE_OTHER_TYPES, OUTPUT_XML_WITH_COMPONENT_VALUE_OTHER_TYPES),
            Arguments.of(INPUT_JSON_WITH_COMPONENT_USER_SELECTED_INTERPRETATION, OUTPUT_XML_WITH_COMPONENT_USER_SELECTED_INTERPRETATION),
            Arguments.of(INPUT_JSON_WITH_COMPONENT_FIRST_INTERPRETATION, OUTPUT_XML_WITH_COMPONENT_FIRST_INTERPRETATION),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_COMPONENTS, OUTPUT_XML_WITH_MULTIPLE_COMPONENTS),
            Arguments.of(INPUT_JSON_WITH_NO_COMPONENT, OUTPUT_XML_WITH_NO_COMPONENT),
            Arguments.of(INPUT_JSON_WITH_PARTICIPANT, OUTPUT_XML_WITH_PARTICIPANT),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_START, OUTPUT_XML_WITH_EFFECTIVE_TIME_UNK_LOW),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD_BLANK, OUTPUT_XML_WITH_EFFECTIVE_TIME_UNK),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE_NO_VALUE_QUANTITY, OUTPUT_XML_WITH_REFERENCE_RANGE_NO_VALUE_QUANTITY),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE_NO_LOW_NO_VALUE_QUANTITY,
                OUTPUT_XML_WITH_REFERENCE_RANGE_NO_LOW_NO_VALUE_QUANTITY),
            Arguments.of(INPUT_JSON_WITH_REFERENCE_RANGE_NO_HIGH_NO_VALUE_QUANTITY,
                OUTPUT_XML_WITH_REFERENCE_RANGE_NO_HIGH_NO_VALUE_QUANTITY),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_END, OUTPUT_XML_WITH_EFFECTIVE_TIME_NO_END),
            Arguments.of(INPUT_JSON_WITH_BODY_SITE, OUTPUT_XML_WITH_BODY_SITE)
        );
    }
}