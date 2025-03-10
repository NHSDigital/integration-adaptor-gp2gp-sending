package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

public class CodeableConceptCdMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/codeableconcept/";
    private static final String TEST_FILE_DIRECTORY_NULL_FLAVOR = "/ehr/mapper/codeableconcept/nullFlavor/";
    private static final String TEST_FILE_DIRECTORY_ACTUAL_PROBLEM = "/ehr/mapper/codeableconcept/actualProblem/";
    private static final String TEST_FILE_DIRECTORY_ALLERGY_RESOLVED = "/ehr/mapper/codeableconcept/allergyResolved/";
    private static final String TEST_FILE_DIRECTORY_ALLERGY_ACTIVE = "/ehr/mapper/codeableconcept/allergyActive/";
    private static final String TEST_FILE_DIRECTORY_MEDICATION = "/ehr/mapper/codeableconcept/medication/";

    private static final String TEST_FILE_TOPIC_RELATED_CONDITION = TEST_FILE_DIRECTORY
        + "topic/codeable_concept_snowmed_related_condtition.json";
    private static final String CD_FOR_TOPIC_RELATED_PROBLEM_AND_TITLE = TEST_FILE_DIRECTORY
        + "topic/cd_for_topic_related_problem_and_title.xml";
    private static final String CD_FOR_TOPIC_TITLE = TEST_FILE_DIRECTORY + "topic/cd_for_topic_title.xml";
    private static final String CD_FOR_TOPIC_UNSPECIFIED = TEST_FILE_DIRECTORY + "topic/cd_for_topic_unspecified.xml";
    private static final String TEST_FILE_DIRECTORY_TOPIC_RELATED_PROBLEM = TEST_FILE_DIRECTORY + "topic/relatedProblem/";
    private static final String CD_FOR_CATEGORY_TITLE = TEST_FILE_DIRECTORY + "category/cd_for_category_titile.xml";
    private static final String CD_FOR_CATEGORY_NO_TITLE = TEST_FILE_DIRECTORY + "category/cd_for_category_no_title.xml";

    private static final String TEST_TITLE = "test title";

    private FhirParseService fhirParseService;
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private static Stream<Arguments> getTestArguments() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY);
    }

    private static Stream<Arguments> getTestArgumentsNullFlavor() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_NULL_FLAVOR);
    }

    private static Stream<Arguments> getTestArgumentsActualProblem() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_ACTUAL_PROBLEM);
    }

    private static Stream<Arguments> getTestArgumentsAllergyResolved() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_ALLERGY_RESOLVED);
    }

    private static Stream<Arguments> getTestArgumentsAllergyActive() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_ALLERGY_ACTIVE);
    }

//    private static Stream<Arguments> getTestArgumentsBloodPressure() {
//        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_BLOOD_PRESSURE);
//    }

    private static Stream<Arguments> getTestArgumentsMedication() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_MEDICATION);
    }

    private static Stream<Arguments> getTestArgumentsForTopicRelatedProblem() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_TOPIC_RELATED_PROBLEM);
    }

    @BeforeEach
    public void setUp() {
        fhirParseService = new FhirParseService();
        codeableConceptCdMapper = new CodeableConceptCdMapper();
    }

    @ParameterizedTest
    @MethodSource("getTestArguments")
    public void When_MappingStubbedCodeableConcept_Expect_HL7CdObjectXml(String inputJson, String outputXml) throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCd(codeableConcept);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    void When_MappingCodeableConceptWithNonSnomedCodeSystems_Expect_ManifestedXmlContainsTranslationsForThoseCodes() {
        var inputJson = """
            {
                "resourceType" : "Observation",
                "code": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "123456",
                            "display": "Endometriosis of uterus"
                        },
                        {
                            "system": "http://read.info/readv2",
                            "code": "READ0",
                            "display": "Read V2 Code Display"
                        },
                        {
                            "system": "http://read.info/ctv3",
                            "code": "READ1",
                            "display": "Read CTV3 Code Display"
                        }
                   ]
                }
            }""";
        var expectedOutputXml = """
            <code code="123456" codeSystem="2.16.840.1.113883.2.1.3.2.4.15" displayName="Endometriosis of uterus">
                <translation code="READ0" codeSystem="2.16.840.1.113883.2.1.6.2" displayName="Read V2 Code Display" />
                <translation code="READ1" codeSystem="2.16.840.1.113883.2.1.3.2.4.14" displayName="Read CTV3 Code Display" />
            </code>""";
        var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

        var outputMessageXml = codeableConceptCdMapper.mapCodeableConceptToCd(codeableConcept);

        assertThat(outputMessageXml).isEqualToIgnoringWhitespace(expectedOutputXml);
    }

    @Test
    void When_MapToNullFlavorCodeableConceptForAllergyWithoutSnomedCode_Expect_OriginalTextIsNotPresent() {
        var inputJson = """
            {
                "resourceType": "AllergyIntolerance",
                "id": "0C1232CF-D34B-4C16-A5F4-0F6461C51A41",
                "meta": {
                    "profile": [
                        "https://fhir.nhs.uk/STU3/StructureDefinition/CareConnect-GPC-AllergyIntolerance-1"
                    ]
                },
                "identifier": [
                    {
                        "system": "https://EMISWeb/A82038",
                        "value": "55D2363D57A248F49A745B2E03F5E93D0C1232CFD34B4C16A5F40F6461C51A41"
                    }
                ],
                "code": {
                    "coding": [
                        {
                            "system": "http://read.info/readv2",
                            "code": "TJ00800",
                            "display": "Adverse reaction to pivampicillin rt"
                        }
                    ],
                    "text": "Adverse reaction to pivampicillin"
                }
            }""";
        var expectedOutputXML = """
            <code nullFlavor="UNK">
            </code>
            """;
        var codeableConcept = fhirParseService.parseResource(inputJson, AllergyIntolerance.class).getCode();

        var outputXml = codeableConceptCdMapper.mapToNullFlavorCodeableConceptForAllergy(
            codeableConcept,
            AllergyIntolerance.AllergyIntoleranceClinicalStatus.ACTIVE
        );

        assertThat(outputXml).isEqualToIgnoringWhitespace(expectedOutputXML);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsActualProblem")
    public void When_MappingStubbedCodeableConceptForActualProblemHeader_Expect_HL7CdObjectXml(String inputJson, String outputXml)
        throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(codeableConcept);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsNullFlavor")
    public void When_MappingStubbedCodeableConceptAsNullFlavor_Expect_HL7CdObjectXml(String inputJson, String outputXml)
        throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapToNullFlavorCodeableConcept(codeableConcept);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsAllergyResolved")
    void When_MappingStubbedCodeableConceptAsResolvedAllergy_Expect_HL7CdObjectXml(String inputJson, String outputXml) {
        var allergyCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(allergyCodeableConcept, AllergyIntolerance.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(codeableConcept,
            AllergyIntolerance.AllergyIntoleranceClinicalStatus.RESOLVED);

        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsAllergyActive")
    void When_MappingStubbedCodeableConceptAsActiveAllergy_Expect_HL7CdObjectXml(String inputJson, String outputXml) {
        var allergyCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(allergyCodeableConcept, AllergyIntolerance.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(codeableConcept,
            AllergyIntolerance.AllergyIntoleranceClinicalStatus.ACTIVE);

        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Nested
    class WhenMappingStubbedCodeableConceptForBloodPressure {
        @Test
        void When_WithoutCoding_Expect_NullFlavorCdXmlWithoutOriginalText() {
            var inputJson = """
                {
                    "resourceType": "Observation"
                }""";
            var expectedOutput = """
            <code nullFlavor="UNK">
            </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithNonSnomedCodingWithDisplay_Expect_NullFlavorCdXmlWithOriginalText() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "display": "Prothrombin time"
                             }
                         ]
                     }
                }""";

            var expectedOutput = """
                <code nullFlavor="UNK">
                    <originalText>Prothrombin time</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithSnomedCodingWithDisplay_Expect_SnomedCdXmlWithOriginalText() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "system": "http://snomed.info/sct",
                                 "display": "Prothrombin time",
                                 "code": "852471000000107"
                             }
                         ]
                     }
                }""";
            var expectedOutput = """
                <code code="852471000000107" codeSystem="2.16.840.1.113883.2.1.3.2.4.15" displayName="Prothrombin time">
                    <originalText>Prothrombin time</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithSnomedCodingWithoutCode_Expect_SnomedCdXmlWithOriginalTextFromText() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "system": "http://snomed.info/sct",
                                 "display": "Prothrombin time",
                                 "code": "852471000000107"
                             }
                         ],
                         "text": "Prothrombin time observed"
                     }
                }""";
            var expectedOutput = """
                <code code="852471000000107" codeSystem="2.16.840.1.113883.2.1.3.2.4.15" displayName="Prothrombin time">
                    <originalText>Prothrombin time observed</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithSnomedCodingWithoutDisplayWithDescriptionExtensionWithoutDisplay_Expect_SnomedCdXml() {
            var inputJson = """
                {
                    "resourceType": "Observation",
                    "code": {
                        "coding": [
                            {
                                "system": "http://snomed.info/sct",
                                "code": "852471000000107",
                                "extension": [
                                    {
                                        "url": "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid",
                                        "extension": [
                                            {
                                                "url": "descriptionId",
                                                "valueId": "12345789"
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                }""";
            var expectedOutput = """
                <code code="852471000000107" codeSystem="2.16.840.1.113883.2.1.3.2.4.15" displayName="">
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithSnomedCodingNoDisplayWithDescriptionExtensionWithDisplay_Expect_SnomedCdXmlWithOriginalTextFromExtensionDisplay() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "system": "http://snomed.info/sct",
                                 "code": "852471000000107",
                                 "extension": [
                                     {
                                         "url": "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid",
                                         "extension": [
                                             {
                                                 "url": "descriptionDisplay",
                                                 "valueString": "Prothrombin time (observed)"
                                             }
                                         ]
                                     }
                                 ]
                             }
                         ]
                     }
                }""";
            var expectedOutput = """
                <code code="852471000000107" codeSystem="2.16.840.1.113883.2.1.3.2.4.15" displayName="">
                    <originalText>Prothrombin time (observed)</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithoutSnomedCodingWithText_Expect_NullFlavorUnkCDWithOriginalTextFromText() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "system": "http://read.info/readv2",
                                 "code": "42Q5.00",
                                 "display": "Prothrombin time"
                             }
                         ],
                         "text": "Prothrombin time (observed)"
                     }
                }""";
            var expectedOutput = """
                <code nullFlavor="UNK">
                    <originalText>Prothrombin time (observed)</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithNonSnomedCodingWithDescriptionExtensionWithoutDisplay_Expect_SnomedCdXmlWithOriginalTextFromDisplay() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "system": "http://read.info/readv2",
                                 "code": "42Q5.00",
                                 "display": "Prothrombin time",
                                 "extension": [
                                     {
                                         "url": "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid",
                                         "extension": [
                                             {
                                                 "url": "descriptioÍnId",
                                                 "valueId": "12345789"
                                             }
                                         ]
                                     }
                                 ]
                             }
                         ]
                     }
                }""";
            var expectedOutput = """
                <code nullFlavor="UNK">
                    <originalText>Prothrombin time</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithNonSnomedCodingWithDescriptionExtensionWithDisplayExtension_Expect_SnomedCdXmlWithOriginalTextFromDisplayExtension() {
            var inputJson = """
                {
                     "resourceType": "Observation",
                     "code": {
                         "coding": [
                             {
                                 "system": "http://read.info/readv2",
                                 "code": "42Q5.00",
                                 "display": "Prothrombin time",
                                 "extension": [
                                     {
                                         "url": "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid",
                                         "extension": [
                                             {
                                                 "url": "descriptionDisplay",
                                                 "valueString": "Prothrombin time (observed)"
                                             }
                                         ]
                                     }
                                 ]
                             }
                         ]
                     }
                }""";
            var expectedOutput = """
                <code nullFlavor="UNK">
                    <originalText>Prothrombin time (observed)</originalText>
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }
    }

    @Nested
    class WhenMappingToNullFlavorCodeableConcept {

        @Test
        void When_WithNonSnomedCodingWithNoDisplayNoTextAndDescriptionExtensionNoDisplayExtension_Expect_SnomedCdXmlWithoutOriginalText() {
            var inputJson = """
                {
                    "resourceType": "Observation",
                    "code": {
                        "coding": [
                            {
                                "system": "http://read.info/readv2",
                                "code": "42Q5.00",
                                "extension": [
                                    {
                                        "url": "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid",
                                        "extension": [
                                            {
                                                "url": "descriptionId",
                                                "valueString": "123456789"
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                }""";

            var expectedOutput = """
                <code nullFlavor="UNK">
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapToNullFlavorCodeableConcept(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }

        @Test
        void When_WithSnomedCodingNoTextNoDisplayWithDescriptionExtensionWithDisplayExtension_Expect_SnomedCdXmlWithoutOriginalText() {
            var inputJson = """
                {
                    "resourceType": "Observation",
                    "code": {
                        "coding": [
                            {
                                "system": "http://snomed.info/sct",
                                "code": "852471000000107",
                                "extension": [
                                    {
                                        "url": "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid",
                                        "extension": [
                                            {
                                                "url": "descriptionDisplay",
                                                "valueString": "Prothrombin time (observed)"
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                }""";

            var expectedOutput = """
                <code nullFlavor="UNK">
                </code>""";
            var codeableConcept = fhirParseService.parseResource(inputJson, Observation.class).getCode();

            var outputMessage = codeableConceptCdMapper.mapToNullFlavorCodeableConcept(codeableConcept);

            assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
        }
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsForTopicRelatedProblem")
    @SneakyThrows
    public void When_MappingCdForTopic_With_RelatedProblem_Expect_HL7CdObjectXml(String inputJson, String outputXml) {
        var condition = ResourceTestFileUtils.getFileContent(inputJson);
        var codeableConcept = fhirParseService.parseResource(condition, Condition.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(codeableConcept);

        assertThat(outputString)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsMedication")
    @SneakyThrows
    public void When_MappingCdForMedication_Expect_HL7CdObjectXml(String inputJson, String outputXml) {
        var medication = ResourceTestFileUtils.getFileContent(inputJson);
        var codeableConcept = fhirParseService.parseResource(medication, Medication.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var outputString = codeableConceptCdMapper.mapCodeableConceptForMedication(codeableConcept);

        assertThat(outputString)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForTopic_With_RelatedProblemAndTitle_Expect_ProblemCodeAndTitleAreUsed() {
        var relatedProblem = ResourceTestFileUtils.getFileContent(TEST_FILE_TOPIC_RELATED_CONDITION);
        var codeableConcept = fhirParseService.parseResource(relatedProblem, Condition.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_RELATED_PROBLEM_AND_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(codeableConcept, TEST_TITLE);

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForTopic_With_TitleOnly_Expect_UnspecifiedProblemAndTitle() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(TEST_TITLE);

        assertThat(outputString)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForTopic_Without_RelatedProblemOrTile_Expect_UnspecifiedProblem() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_UNSPECIFIED);
        var outputString = codeableConceptCdMapper.getCdForTopic();

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForCategory_With_Title_Expect_OtherCategoryAndOriginalText() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_CATEGORY_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForCategory(TEST_TITLE);

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_GetCdForCategory_Expect_OtherCategory() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_CATEGORY_NO_TITLE);
        var outputString = codeableConceptCdMapper.getCdForCategory();

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForMedication_With_RelatedProblemAndTitle_Expect_ConceptIdAndTitle() {
        var relatedProblem = ResourceTestFileUtils.getFileContent(TEST_FILE_TOPIC_RELATED_CONDITION);
        var codeableConcept = fhirParseService.parseResource(relatedProblem, Condition.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_RELATED_PROBLEM_AND_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(codeableConcept, TEST_TITLE);

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }
}