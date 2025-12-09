package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class EncounterMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/encounter/";
    private static final String TEST_ID = "test-id";
    private static final String CONSULTATION_REFERENCE = "F550CC56-EF65-4934-A7B1-3DC2E02243C3";
    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final Date CONSULTATION_DATE = Date.from(Instant.parse("2010-01-13T15:13:32Z"));
    private static final String TEST_LOCATION_NAME = "Test Location";
    private static final String TEST_LOCATION_ID = "EB3994A6-5A87-4B53-A414-913137072F57";
    private static final String CONFIDENTIALITY_CODE = """
        <confidentialityCode
            code="NOPAT"
            codeSystem="2.16.840.1.113883.4.642.3.47"
            displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
        />""";

    public static final Bundle.BundleEntryComponent BUNDLE_ENTRY_WITH_CONSULTATION = new Bundle.BundleEntryComponent()
        .setResource(new ListResource()
            .setEncounter(new Reference()
                .setReference(CONSULTATION_REFERENCE))
            .setCode(new CodeableConcept()
                .setCoding(List.of(new Coding()
                    .setCode(CONSULTATION_LIST_CODE))))
            .setDate(CONSULTATION_DATE));

    public static final Bundle.BundleEntryComponent BUNDLE_ENTRY_WITH_LOCATION = new Bundle.BundleEntryComponent()
        .setResource(new Location().setName(TEST_LOCATION_NAME).setId(TEST_LOCATION_ID));

    private static final String SAMPLE_EHR_COMPOSITION_COMPONENT = TEST_FILES_DIRECTORY
        + "sample-ehr-composition-component.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private EncounterComponentsMapper encounterComponentsMapper;
    @Mock
    private Bundle bundle;

    @Mock
    private ConfidentialityService confidentialityService;

    private EncounterMapper encounterMapper;
    private MessageContext messageContext;

    @BeforeEach
    void setUp() {
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper, confidentialityService);
    }

    @AfterEach
    void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    void testEncounterWithNOPATAddsConfidentialityCodeIntoEhrComposition() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        when(bundle.getEntry()).thenReturn(List.of(
                BUNDLE_ENTRY_WITH_CONSULTATION,
                BUNDLE_ENTRY_WITH_LOCATION
        ));

        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);
        var encounterJsonInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + "input-encounter-with-nopat.json");
        var expectedOutputWithConfidentialityCode
                            = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + "output-with-confidentiality-code.xml");

        Encounter parsedEncounter = new FhirParseService().parseResource(encounterJsonInput, Encounter.class);
        when(confidentialityService.generateConfidentialityCode(parsedEncounter)).thenReturn(Optional.of(CONFIDENTIALITY_CODE));
        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        String outputMessageWithConfidentialityCode = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);

        assertThat(outputMessageWithConfidentialityCode).isEqualTo(expectedOutputWithConfidentialityCode);
        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }

    @ParameterizedTest
    @MethodSource("testFilePaths")
    void When_MappingParsedEncounterJson_Expect_EhrCompositionXmlOutput(String input, String output) {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);
        when(bundle.getEntry()).thenReturn(List.of(
                BUNDLE_ENTRY_WITH_CONSULTATION,
                BUNDLE_ENTRY_WITH_LOCATION
        ));

        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + output);

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + input);
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);
        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        String outputMessage = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);

        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }
    @Test
    void When_MappingParsedEncounterJson_Expect_EhrCompositionXmlOutputWithNulAuthor() {
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);
        when(bundle.getEntry()).thenReturn(List.of(
            BUNDLE_ENTRY_WITH_CONSULTATION,
            BUNDLE_ENTRY_WITH_LOCATION
        ));

        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        String expectedOutputMessage = ResourceTestFileUtils
                .getFileContent(TEST_FILES_DIRECTORY + "output-with-nul-author-participant2.xml");

        var jsonInput = ResourceTestFileUtils
                .getFileContent(TEST_FILES_DIRECTORY + "input-without-recorder-participant.json");
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);
        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        String outputMessage = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);

        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of("input-with-effective-time.json", "output-with-effective-time.xml"),
            Arguments.of("input-with-start-effective-time.json", "output-with-start-effective-time.xml"),
            Arguments.of("input-with-no-effective-time.json", "output-with-no-effective-time.xml"),
            Arguments.of("input-with-no-period-field.json", "output-with-no-period-field.xml"),
            Arguments.of("input-with-type-snomed-and-in-vocab-and-text.json", "output-with-type-snomed-and-in-vocab-and-text.xml"),
            Arguments.of("input-with-type-snomed-and-in-vocab-and-no-text.json", "output-with-type-snomed-and-in-vocab-and-no-text.xml"),
            Arguments.of("input-with-type-snomed-and-not-in-vocab-and-text.json", "output-with-type-snomed-and-not-in-vocab-and-text.xml"),
            Arguments.of("input-with-type-snomed-and-not-in-vocab-no-text.json", "output-with-type-snomed-and-not-in-vocab-no-text.xml"),
            Arguments.of("input-with-type-not-snomed-and-text.json", "output-with-type-not-snomed-and-text.xml"),
            Arguments.of("input-with-type-not-snomed-and-no-text.json", "output-with-type-not-snomed-and-no-text.xml"),
            Arguments.of("input-with-type-and-no-coding-and-text.json", "output-with-type-and-no-coding-and-text.xml"),
            Arguments.of(
                "input-with-type-and-no-coding-and-text-and-no-text.json",
                "output-with-type-and-no-coding-and-text-and-no-text.xml"
            ),
            Arguments.of("input-without-performer-participant.json", "output-with-recorder-as-participant2.xml"),
            Arguments.of("input-with-no-location-reference.json", "output-with-no-location-reference.xml")
        );
    }

    @Test
    void When_MappingEncounterWithNoType_Expect_Exception() {
        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + "input-with-no-type.json");

        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        assertThatThrownBy(() -> encounterMapper.mapEncounterToEhrComposition(parsedEncounter))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Could not map Encounter type");
    }

    @Test
    void When_MappingEncounterLocationWithNoReference_Expect_Exception() {
        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY
                                                             + "input-encounter-with-location-with-no-reference.json");
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);
        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        assertThatThrownBy(() -> encounterMapper.mapEncounterToEhrComposition(parsedEncounter))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Resource not found: Location/EB3994A6-5A87-4B53-A414-913137072F57");
    }

    @Test
    void When_MappingEncounterWithInvalidParticipantReferenceResourceType_Expect_Exception() {
        when(bundle.getEntry()).thenReturn(List.of(
                BUNDLE_ENTRY_WITH_CONSULTATION,
                BUNDLE_ENTRY_WITH_LOCATION
        ));

        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY
            + "input-with-performer-invalid-reference-resource-type.json");

        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        assertThatThrownBy(() -> encounterMapper.mapEncounterToEhrComposition(parsedEncounter))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Not supported agent reference: Patient/6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73");
    }

    @Test
    void When_MappingEmptyConsultation_Expect_NoEhrCompositionGenerated() {
        var jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY
            + "input-with-no-associated-consultation.json");
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        String outputMessage = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);
        assertThat(outputMessage).isEqualTo(StringUtils.EMPTY);

        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }
}
