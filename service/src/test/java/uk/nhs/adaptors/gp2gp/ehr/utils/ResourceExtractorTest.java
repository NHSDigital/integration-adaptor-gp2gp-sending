package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceExtractorTest {

    private static final String LIST_RESOURCE_TEST_FILE_DIRECTORY = "/ehr/mapper/listresource/";
    private static final String BUNDLE_FILE = "fhir_bundle.json";
    private static final String LIST_REFERENCE_ID = "List/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac02";

    private final FhirParseService fhirParseService = new FhirParseService();

    private Bundle parseBundle() {
        var jsonInput = ResourceTestFileUtils.getFileContent(LIST_RESOURCE_TEST_FILE_DIRECTORY + BUNDLE_FILE);
        return fhirParseService.parseResource(jsonInput, Bundle.class);
    }

    private IIdType createAllergyIntoleranceReference(String refId) {
        var allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.getAsserter().setReference(refId);
        return allergyIntolerance.getAsserter().getReferenceElement();
    }

    private Optional<Resource> extractByReference(Bundle bundle, String referenceId) {
        return ResourceExtractor.extractResourceByReference(bundle, createAllergyIntoleranceReference(referenceId));
    }

    @Test
    void When_ExtractingListWithContainedAllergiesResourceByReference_Expect_ResourceFound() {
        var allBundle = parseBundle();
        Optional<Resource> resource = extractByReference(allBundle, LIST_REFERENCE_ID);

        assertTrue(resource.isPresent());
        Resource extracted = resource.get();
        ListResource listResource = assertInstanceOf(ListResource.class, extracted);
        assertAll(
            () -> assertEquals("List/ended-allergies", extracted.getId()),
            () -> assertEquals(2, listResource.getContained().size())
        );
    }

    @Test
    void When_ExtractingResourceWithValidContainedIdButInvalidResourceTypeReference_Expect_EmptyResult() {
        final String WRONG_TYPE_REFERENCE_ID = "Organization/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac02";

        var allBundle = parseBundle();
        Optional<Resource> resources = extractByReference(allBundle, WRONG_TYPE_REFERENCE_ID);

        assertFalse(resources.isPresent());
    }

    @Test
    void When_ExtractingResourceByNonExistentReference_Expect_EmptyResult() {
        final String NON_EXISTENT_REFERENCE_ID = "List/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac04";

        var allBundle = parseBundle();
        Optional<Resource> resources = extractByReference(allBundle, NON_EXISTENT_REFERENCE_ID);

        assertFalse(resources.isPresent());
    }
}
