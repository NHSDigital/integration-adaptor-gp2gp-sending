package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class InputBundleTest {

    private static final String INPUT_BUNDLE_PATH = "/ehr/mapper/input-bundle.json";
    private static final String EXISTING_REFERENCE = "Appointment/1234";
    private static final String IGNORED_REFERENCE = "QuestionnaireResponse/12345";
    private static final String NO_EXISTING_REFERENCE = "Encounter/3543676";
    private static final String UNSUPPORTED_RESOURCE_REFERENCE = "Binary/3543676";
    private static final String ENCOUNTER_LIST_EXISTING_REFERENCE = "Encounter/43536547";
    private static final String ENCOUNTER_LIST_NOT_EXISTING_REFERENCE = "Encounter/123456";
    private static final String EXISTING_LIST_REFERENCE = "List/0123456";
    private static final String EXISTING_DIAGNOSTIC_REPORT_REFERENCE_1 = "DiagnosticReport/54321";
    private static final String INVALID_CODE = "not-valid-code";
    private static final String VALID_CODE = "valid-code";

    private Bundle bundle;

    @BeforeEach
    public void setUp() {
        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_BUNDLE_PATH);
        bundle = new FhirParseService().parseResource(inputJson, Bundle.class);
    }

    @Test
    public void When_GettingListOfResourcesOfType_Expect_ListReturned() {
        final var className = DiagnosticReport.class;
        final var resourcesList = new InputBundle(bundle).getResourcesOfType(className);

        assertThat(resourcesList).isNotEmpty();
        assertThat(resourcesList.get(0).getId()).isEqualTo(EXISTING_DIAGNOSTIC_REPORT_REFERENCE_1);
        assertThat(resourcesList.get(0).getResourceType()).isEqualTo(ResourceType.valueOf(className.getSimpleName()));
    }

    @Test
    public void When_GettingResourceFromBundle_Expect_ResourceReturned() {
        Optional<Resource> resource = new InputBundle(bundle).getResource(new IdType(EXISTING_REFERENCE));

        assertThat(resource).isPresent();
        assertThat(resource.get().getId()).isEqualTo(EXISTING_REFERENCE);
        assertThat(resource.get().getResourceType()).isEqualTo(ResourceType.Appointment);
    }

    @Test
    public void When_GettingIgnoredResourceFromBundle_Expect_ResourceIgnored() {
        Optional<Resource> resource = new InputBundle(bundle).getResource(new IdType(IGNORED_REFERENCE));

        assertThat(resource).isNotPresent();
    }

    @Test
    public void When_GettingResourceFromEmptyBundle_Expect_EhrMapperExceptionThrown() {
        assertThatThrownBy(() -> new InputBundle(new Bundle()).getResource(new IdType(EXISTING_REFERENCE)))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Resource not found: " + EXISTING_REFERENCE);
    }

    @Test
    public void When_GettingNotInBundleResource_Expect_EhrMapperExceptionThrown() {
        assertThatThrownBy(() -> new InputBundle(new Bundle()).getResource(new IdType(NO_EXISTING_REFERENCE)))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Resource not found: " + NO_EXISTING_REFERENCE);
    }

    @Test
    public void When_RetrievingUnmappableResource_Expect_ErrorThrown() {
        assertThatThrownBy(() -> new InputBundle(bundle).getResource(new IdType(UNSUPPORTED_RESOURCE_REFERENCE)))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Reference not supported resource type: " + UNSUPPORTED_RESOURCE_REFERENCE);
    }

    @Test
    public void When_GettingListReferencedToEncounter_Expect_ValidListResourceReturned() {
        Optional<ListResource> listReferencedToEncounter =
            new InputBundle(bundle).getListReferencedToEncounter(new IdType(ENCOUNTER_LIST_EXISTING_REFERENCE), VALID_CODE);

        assertThat(listReferencedToEncounter).isPresent();
        assertThat(listReferencedToEncounter.get().getId()).isEqualTo(EXISTING_LIST_REFERENCE);
        assertThat(listReferencedToEncounter.get().getResourceType()).isEqualTo(ResourceType.List);
    }

    @Test
    public void When_GettingListReferencedWithNotExistingCode_Expect_NoneListResourceReturned() {
        Optional<ListResource> listReferencedToEncounter =
            new InputBundle(bundle).getListReferencedToEncounter(new IdType(ENCOUNTER_LIST_EXISTING_REFERENCE), INVALID_CODE);
        assertThat(listReferencedToEncounter).isNotPresent();
    }

    @Test
    public void When_GettingListNoReferencedToValidEncounter_Expect_NoneListResourceReturned() {
        Optional<ListResource> listReferencedToEncounter =
            new InputBundle(bundle).getListReferencedToEncounter(new IdType(ENCOUNTER_LIST_NOT_EXISTING_REFERENCE), VALID_CODE);
        assertThat(listReferencedToEncounter).isNotPresent();
    }

    @Test
    public void When_GettingListWithEmptyReference_Expect_NoneListResourceReturned() {
        assertThat(new InputBundle(bundle).getListReferencedToEncounter(new IdType(), VALID_CODE)).isNotPresent();
    }

    @Test
    public void When_GettingListWithNullReference_Expect_NoneListResourceReturned() {
        assertThat(new InputBundle(bundle).getListReferencedToEncounter(null, VALID_CODE)).isNotPresent();
    }

    @Test
    public void When_GettingPractitionerRoleForPractitionerAndOrganization_Expect_PractitionerRoleReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/1", "Organization/2");
        assertThat(practitionerRoleFor).isPresent();
    }

    @Test
    public void When_GettingPractitionerRoleForPractitionerAndOrganizationThatDoesNotMatch_Expect_EmptyReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/not-match", "Organization/not-match");
        assertThat(practitionerRoleFor).isEmpty();
    }

    @Test
    public void When_GettingPractitionerRoleWithoutPractitionerAndOrganization_Expect_EmptyReturned() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new PractitionerRole());
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/1", "Organization/2");
        assertThat(practitionerRoleFor).isEmpty();
    }

    @Test
    public void When_GettingPractitionerRoleWhereOnlyOrganizationMatch_Expect_EmptyReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/not-match", "Organization/2");
        assertThat(practitionerRoleFor).isEmpty();
    }

    @Test
    public void When_GettingPractitionerRoleWhereOnlyPractitionerMatch_Expect_EmptyReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/1", "Organization/not-match");
        assertThat(practitionerRoleFor).isEmpty();
    }
}
