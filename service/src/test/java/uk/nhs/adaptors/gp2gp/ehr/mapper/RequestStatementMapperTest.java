package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;

import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;

import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class RequestStatementMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/referral/";

    // INPUT FILES
    private static final String INPUT_JSON_BUNDLE = TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "example-referral-request-no-optional-fields.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "example-referral-request-with-optional-fields.json";
    private static final String INPUT_JSON_WITH_ONE_REASON_CODE = TEST_FILE_DIRECTORY
        + "example-referral-request-with-one-reason-code.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-practitioner-requester.json";
    private static final String INPUT_JSON_WITH_REASON_CODES = TEST_FILE_DIRECTORY
        + "example-referral-request-with-reason-codes.json";
    private static final String INPUT_JSON_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY
        + "example-referral-request-with-services-requested.json";
    private static final String INPUT_JSON_WITH_DEVICE_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-device-requester.json";
    private static final String INPUT_JSON_WITH_ORG_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-org-requester.json";
    private static final String INPUT_JSON_WITH_PATIENT_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-patient-requester.json";
    private static final String INPUT_JSON_WITH_RELATION_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-relation-requester.json";
    private static final String INPUT_JSON_WITH_ONE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-with-one-practitioner-recipient.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-with-multiple-practitioner-recipients.json";
    private static final String INPUT_JSON_WITH_NOTES = TEST_FILE_DIRECTORY
        + "example-referral-request-with-notes.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-with-incorrect-resource-type-recipient.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_AUTHOR = TEST_FILE_DIRECTORY
        + "example-referral-request-with-incorrect-resource-type-author.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-incorrect-resource-type-requester.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-no-resolved-reference-requester.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-no-resolved-reference-recipient.json";
    private static final String INPUT_JSON_WITH_NOPAT = TEST_FILE_DIRECTORY + "example-referral-request-with-nopat.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_NOTE_AUTHOR = TEST_FILE_DIRECTORY
        + "example-referral-request-no-resolved-reference-note-author.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF = TEST_FILE_DIRECTORY
        + "example-referral-request-no-onbehalfof.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_RECIPIENTS = TEST_FILE_DIRECTORY
        + "example-referral-request-with-multiple-recipients.json";
    private static final String INPUT_JSON_WITH_ASAP_PRIORITY = TEST_FILE_DIRECTORY
        + "example-referral-request-with-asap-priority.json";
    private static final String INPUT_JSON_WITH_ROUTINE_PRIORITY = TEST_FILE_DIRECTORY
        + "example-referral-request-with-routine-priority.json";
    private static final String INPUT_JSON_WITH_URGENT_PRIORITY = TEST_FILE_DIRECTORY
        + "example-referral-request-with-urgent-priority.json";
    private static final String INPUT_JSON_WITH_UNSUPPORTED_PRIORITY = TEST_FILE_DIRECTORY
            + "example-referral-request-with-unsupported-priority.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE = TEST_FILE_DIRECTORY
        + "example-referral-request-supportingInfo-with-document-reference.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_DESCRIPTION = TEST_FILE_DIRECTORY
        + "example-referral-request-supportingInfo-with-document-reference-no-description.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_CREATED_NO_TEXT = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-document-reference-no-created-no-text.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-observation.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION_EFFECTIVEPERIOD = TEST_FILE_DIRECTORY
        + "example-referral-request-supportingInfo-with-observation-effectivePeriod.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION_EFFECTIVEPERIOD_NO_START = TEST_FILE_DIRECTORY
        + "example-referral-request-supportingInfo-with-observation-effectivePeriod-no-start.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION_NO_DATE = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-observation-no-date.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_REFERRALREQUEST = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-referral-request.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_REFERRALREQUEST_NO_DATE_NO_REASON = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-referral-request-no-date-no-reason.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-diagnostic-report.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT_NO_DATE_NO_SYSTEM = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-diagnostic-report-no-date-no-system.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_MEDICATIONREQUEST = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-with-medication-request.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_MEDICATIONREQUEST_NO_DATE = TEST_FILE_DIRECTORY
        + "example-referral-request-supportingInfo-with-medication-request-no-date.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_MEDICATION_REQUEST_WITH_MEDICATION_CODEABLE_CONCEPT = TEST_FILE_DIRECTORY
            + "example-referral-request-supportingInfo-medication-request-with-medication-codeable-concept.json";
    private static final String INPUT_JSON_WITH_SUPPORTINGINFO_IGNORED_RESOURCES = TEST_FILE_DIRECTORY
        + "example-referral-request-supportingInfo-with-ignored-resources.json";
    private static final String INPUT_JSON_WITH_NO_AUTHOR_AND_TIME = TEST_FILE_DIRECTORY
        + "example-referral-request-no-author-and-time.json";
    private static final String INPUT_JSON_WITH_WITH_UBR_NUMBER_SYSTEM_URL = TEST_FILE_DIRECTORY
            + "example-referral-request-with-ubr-number-system-url.json";
    private static final String INPUT_JSON_WITH_WITH_UBRN_SYSTEM_URL = TEST_FILE_DIRECTORY
            + "example-referral-request-with-ubrn-system-url.json";

    // OUTPUT FILES
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-optional-fields.xml";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS_NESTED = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-optional-fields-nested.xml";
    private static final String OUTPUT_XML_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-optional-fields.xml";
    private static final String OUTPUT_XML_WITH_ONE_REASON_CODE = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-one-reason-code.xml";
    private static final String OUTPUT_XML_WITH_PRACTITIONER_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-practitioner-requester.xml";
    private static final String OUTPUT_XML_WITH_REASON_CODES = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-reason-codes.xml";
    private static final String OUTPUT_XML_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-services-requested.xml";
    private static final String OUTPUT_XML_WITH_DEVICE_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-device-requester.xml";
    private static final String OUTPUT_XML_WITH_ORG_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-org-requester.xml";
    private static final String OUTPUT_XML_WITH_PATIENT_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-patient-requester.xml";
    private static final String OUTPUT_XML_WITH_RELATION_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-relation-requester.xml";
    private static final String OUTPUT_XML_WITH_ONE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-one-practitioner-recipient.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-multiple-practitioner-recipients.xml";
    private static final String OUTPUT_XML_WITH_NOTES = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-notes.xml";
    private static final String OUTPUT_XML_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-onbehalfof.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_RECIPIENTS = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-multiple-recipients.xml";
    private static final String OUTPUT_XML_WITH_NORMAL_PRIORITY = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-normal-priority.xml";
    private static final String OUTPUT_XML_WITH_HIGH_PRIORITY = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-high-priority.xml";
    private static final String OUTPUT_XML_WITH_PRIORITY_ASAP = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-text-priority-asap.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-supportingInfo-with-document-reference.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_DESCRIPTION = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-supportingInfo-with-document-reference-no-description.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_CREATED_NO_TEXT = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-document-reference-no-created-no-text.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_OBSERVATION = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-observation.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_OBSERVATION_NO_DATE = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-observation-no-date.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_REFERRALREQUEST = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-referral-request.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_REFERRALREQUEST_NO_DATE_NO_REASON = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-referral-request-no-date-no-reason.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-diagnostic-report.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT_NO_DATE_NO_SYSTEM = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-diagnostic-report-no-date-no-system.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_MEDICATIONREQUEST = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-with-medication-request.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_MEDICATIONREQUEST_NO_DATE = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-supportingInfo-with-medication-request-no-date.xml";
    private static final String OUTPUT_XML_WITH_SUPPORTINGINFO_FROM_MEDICATION_CODEABLE_CONCEPT_DISPLAY = TEST_FILE_DIRECTORY
            + "expected-output-request-statement-supportingInfo-from-medication-codeable-concept-display.xml";
    private static final String OUTPUT_XML_WITH_NO_SUPPORTINGINFO = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-supportingInfo.xml";
    private static final String OUTPUT_XML_WITH_NO_AUTHOR_AND_TIME = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-author-and-time.xml";
    private static final String OUTPUT_XML_WITH_SYSTEM_URL = TEST_FILE_DIRECTORY
            + "expected-output-request-for-system-url.xml";

    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private MessageContext messageContext;
    @Mock
    private IdMapper idMapper;
    @Mock
    private AgentDirectory agentDirectory;
    @Mock
    private ConfidentialityService confidentialityService;

    private RequestStatementMapper requestStatementMapper;

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            arguments(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            arguments(INPUT_JSON_WITH_OPTIONAL_FIELDS, OUTPUT_XML_WITH_OPTIONAL_FIELDS),
            arguments(INPUT_JSON_WITH_PRACTITIONER_REQUESTER, OUTPUT_XML_WITH_PRACTITIONER_REQUESTER),
            arguments(INPUT_JSON_WITH_SERVICES_REQUESTED, OUTPUT_XML_WITH_SERVICES_REQUESTED),
            arguments(INPUT_JSON_WITH_DEVICE_REQUESTER, OUTPUT_XML_WITH_DEVICE_REQUESTER),
            arguments(INPUT_JSON_WITH_ORG_REQUESTER, OUTPUT_XML_WITH_ORG_REQUESTER),
            arguments(INPUT_JSON_WITH_PATIENT_REQUESTER, OUTPUT_XML_WITH_PATIENT_REQUESTER),
            arguments(INPUT_JSON_WITH_RELATION_REQUESTER, OUTPUT_XML_WITH_RELATION_REQUESTER),
            arguments(INPUT_JSON_WITH_ONE_PRACTITIONER_RECIPIENT, OUTPUT_XML_WITH_ONE_PRACTITIONER_RECIPIENT),
            arguments(INPUT_JSON_WITH_MULTIPLE_PRACTITIONER_RECIPIENT, OUTPUT_XML_WITH_MULTIPLE_PRACTITIONER_RECIPIENT),
            arguments(INPUT_JSON_WITH_NOTES, OUTPUT_XML_WITH_NOTES),
            arguments(INPUT_JSON_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF, OUTPUT_XML_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF),
            arguments(INPUT_JSON_WITH_MULTIPLE_RECIPIENTS, OUTPUT_XML_WITH_MULTIPLE_RECIPIENTS),
            arguments(INPUT_JSON_WITH_ASAP_PRIORITY, OUTPUT_XML_WITH_PRIORITY_ASAP),
            arguments(INPUT_JSON_WITH_ROUTINE_PRIORITY, OUTPUT_XML_WITH_NORMAL_PRIORITY),
            arguments(INPUT_JSON_WITH_URGENT_PRIORITY, OUTPUT_XML_WITH_HIGH_PRIORITY),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_DESCRIPTION,
                OUTPUT_XML_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_DESCRIPTION),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_CREATED_NO_TEXT,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_DOCUMENTREFERENCE_NO_CREATED_NO_TEXT),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_OBSERVATION),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION_EFFECTIVEPERIOD,
                OUTPUT_XML_WITH_SUPPORTINGINFO_OBSERVATION),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION_EFFECTIVEPERIOD_NO_START,
                OUTPUT_XML_WITH_SUPPORTINGINFO_OBSERVATION_NO_DATE),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_OBSERVATION_NO_DATE,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_OBSERVATION_NO_DATE),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_REFERRALREQUEST,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_REFERRALREQUEST),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_REFERRALREQUEST_NO_DATE_NO_REASON,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_REFERRALREQUEST_NO_DATE_NO_REASON),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT_NO_DATE_NO_SYSTEM,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_DIAGNOSTICREPORT_NO_DATE_NO_SYSTEM),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_MEDICATIONREQUEST,
                    OUTPUT_XML_WITH_SUPPORTINGINFO_MEDICATIONREQUEST),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_MEDICATIONREQUEST_NO_DATE,
                OUTPUT_XML_WITH_SUPPORTINGINFO_MEDICATIONREQUEST_NO_DATE),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_IGNORED_RESOURCES,
                OUTPUT_XML_WITH_NO_SUPPORTINGINFO),
            arguments(INPUT_JSON_WITH_WITH_UBRN_SYSTEM_URL, OUTPUT_XML_WITH_SYSTEM_URL),
            arguments(INPUT_JSON_WITH_SUPPORTINGINFO_MEDICATION_REQUEST_WITH_MEDICATION_CODEABLE_CONCEPT,
                OUTPUT_XML_WITH_SUPPORTINGINFO_FROM_MEDICATION_CODEABLE_CONCEPT_DISPLAY),
            arguments(INPUT_JSON_WITH_NO_AUTHOR_AND_TIME, OUTPUT_XML_WITH_NO_AUTHOR_AND_TIME),
            arguments(INPUT_JSON_WITH_WITH_UBR_NUMBER_SYSTEM_URL, OUTPUT_XML_WITH_SYSTEM_URL)
        );
    }

    private static Stream<Arguments> resourceFileParamsReasonCodes() {
        return Stream.of(
            arguments(INPUT_JSON_WITH_ONE_REASON_CODE, OUTPUT_XML_WITH_ONE_REASON_CODE),
            arguments(INPUT_JSON_WITH_REASON_CODES, OUTPUT_XML_WITH_REASON_CODES)
        );
    }

    private static Stream<Arguments> resourceFileParamsWithInvalidData() {
        return Stream.of(
            arguments(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_REQUESTER, "Requester Reference not of expected Resource Type"),
            arguments(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_RECIPIENT, "Recipient Reference not of expected Resource Type"),
            arguments(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_AUTHOR, "Author Reference not of expected Resource Type"),
            arguments(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_REQUESTER, "Resource not found: Device/un-resolved"),
            arguments(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_RECIPIENT, "Resource not found: Organization/un-resolved"),
            arguments(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_NOTE_AUTHOR, "Resource not found: RelatedPerson/un-resolved"),
            arguments(INPUT_JSON_WITH_UNSUPPORTED_PRIORITY, "Unsupported priority in ReferralRequest: stat")
        );
    }

    @BeforeEach
    public void setUp() {
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        var inputBundle = new InputBundle(bundle);

        lenient().when(messageContext.getIdMapper()).thenReturn(idMapper);
        lenient().when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        lenient().when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);
        lenient().when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenAnswer(mockIdForResourceAndId());
        lenient().when(idMapper.getOrNew(any(Reference.class))).thenAnswer(mockIdForReference());
        lenient().when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockIdForReference());
        lenient().when(agentDirectory.getAgentRef(any(Reference.class), any(Reference.class))).thenAnswer(mockIdForAgentReference());

        requestStatementMapper = new RequestStatementMapper(messageContext,
                                                            codeableConceptCdMapper,
                                                            new ParticipantMapper(),
                                                            confidentialityService);
    }

    private Answer<String> mockIdForResourceAndId() {
        return invocation -> {
            ResourceType resourceType = invocation.getArgument(0);
            IdType idType = invocation.getArgument(1);
            return String.format("II-for-%s-%s", resourceType, idType.getIdPart());
        };
    }

    private Answer<String> mockIdForReference() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("II-for-%s", reference.getReference());
        };
    }

    private Answer<String> mockIdForAgentReference() {
        return invocation -> {
            Reference practitionerReference = invocation.getArgument(0);
            Reference organizationReference = invocation.getArgument(1);
            return String.format("II-for-%s-%s", practitionerReference.getReference(), organizationReference.getReference());
        };
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        assertThatInputMapsToExpectedOutput(inputJson, outputXml);
    }

    @SneakyThrows
    private void assertThatInputMapsToExpectedOutput(String inputJsonResourcePath, String outputXmlResourcePath) {
        var expected = ResourceTestFileUtils.getFileContent(outputXmlResourcePath);
        var input = ResourceTestFileUtils.getFileContent(inputJsonResourcePath);
        var referralRequest = new FhirParseService().parseResource(input, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(referralRequest, false);

        assertThat(outputMessage).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsReasonCodes")
    public void When_MappingObservationJsonWithReason_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        assertThatInputMapsToExpectedOutput(inputJson, outputXml);
    }

    @Test
    public void When_MappingReferralRequestJsonWithNestedTrue_Expect_RequestStatementXmlOutput() {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NO_OPTIONAL_FIELDS_NESTED);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, true);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @Test
    public void When_MappingReferralRequestWithNoPat_Expect_RequestStatementWithConfidentialityCode() {
        when(confidentialityService.generateConfidentialityCode(any(ReferralRequest.class)))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NOPAT);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, true);

        assertThat(outputMessage).contains(NOPAT_HL7_CONFIDENTIALITY_CODE);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsWithInvalidData")
    public void When_MappingReferralRequestJsonWithInvalidData_Expect_Exception(String inputJson, String exceptionMessage) {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        assertThatThrownBy(() -> requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage(exceptionMessage);
    }
}
