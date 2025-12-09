package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ResourceExtractorTest {

    private static final String LIST_RESOURCE_TEST_FILE_DIRECTORY = "/ehr/mapper/listresource/";
    private static final String LIST_REFERENCE_ID =
            "List/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac02";

    private final FhirParseService fhirParseService = new FhirParseService();

    private Bundle parseBundle(String fileName) {
        var jsonInput = ResourceTestFileUtils.getFileContent(LIST_RESOURCE_TEST_FILE_DIRECTORY + fileName);
        return fhirParseService.parseResource(jsonInput, Bundle.class);
    }

    private IIdType createAllergyIntoleranceReference(String refId) {
        var allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.getAsserter().setReference(refId);
        return allergyIntolerance.getAsserter().getReferenceElement();
    }

    @Test
    void When_ExtractingListWithContainedAllergiesResourceByReference_Expect_ResourceFound() {
        var allBundle = parseBundle("fhir_bundle.json");

        IIdType reference = createAllergyIntoleranceReference(LIST_REFERENCE_ID);
        Optional<Resource> resources = ResourceExtractor.extractResourceByReference(allBundle, reference);

        assertEquals(1, resources.stream().count());
        assertEquals("List/ended-allergies", resources.get().getId());
        assertEquals(2, ((ListResource) resources.get()).getContained().size());
    }

    @Test
    void When_ExtractingResourceWithValidContainedIdButInvalidResourceTypeReference_Expect_EmptyResult() {
        final String WRONG_TYPE_REFERENCE_ID =
                "Organization/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac02";

        var allBundle = parseBundle("fhir_bundle.json");

        IIdType reference = createAllergyIntoleranceReference(WRONG_TYPE_REFERENCE_ID);
        Optional<Resource> resources = ResourceExtractor.extractResourceByReference(allBundle, reference);

        assertEquals(Optional.empty(), resources);
    }

    @Test
    void When_ExtractingResourceByNonExistentReference_Expect_EmptyResult() {
        final String NON_EXISTENT_REFERENCE_ID =
                "List/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac04";

        var allBundle = parseBundle("fhir_bundle.json");

        IIdType reference = createAllergyIntoleranceReference(NON_EXISTENT_REFERENCE_ID);
        Optional<Resource> resources = ResourceExtractor.extractResourceByReference(allBundle, reference);

        assertEquals(Optional.empty(), resources);
    }
}
