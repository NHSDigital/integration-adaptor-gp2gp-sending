package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusValidator;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

class EhrExtractStatusValidatorTest {

    private static final String OBJECT_NAME = "some-file-name";
    private static final String PATIENT_ID = "3";

    @Test
    void When_AllPreparingDataStepsAreFinished_Expect_ReturnTrue() {
        var ehrExtractStatus = buildEhrExtractStatus(List.of(getFinishedGpcDocument(), getFinishedGpcDocument()),
            getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("all document and structured access steps are complete")
            .isTrue();
    }

    private EhrExtractStatus.GpcDocument getFinishedGpcDocument() {
        return getGpcDocument(OBJECT_NAME);
    }

    private EhrExtractStatus.GpcAccessStructured getFinishedGpcAccessStructured() {
        return getGpcAccessStructured(OBJECT_NAME);
    }

    private EhrExtractStatus.GpcDocument getGpcDocument(String objectName) {
        return EhrExtractStatus.GpcDocument.builder()
            .objectName(objectName)
            .build();
    }

    private EhrExtractStatus.GpcAccessStructured getGpcAccessStructured(String objectName) {
        return EhrExtractStatus.GpcAccessStructured.builder()
            .objectName(objectName)
            .build();
    }

    @Test
    void When_AllPreparingDataStepsAreFinishedAndDocumentsListIsEmpty_Expect_ReturnTrue() {
        var ehrExtractStatus = buildEhrExtractStatus(List.of(), getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("empty documents list is valid when structured access is complete")
            .isTrue();
    }

    @Test
    void When_AllPreparingDataStepsNotFinished_Expect_ReturnFalse() {
        var ehrExtractStatus = buildEhrExtractStatus(List.of(getUnfinishedGpcDocument(), getUnfinishedGpcDocument()),
            getUnfinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("both document and structured access steps are incomplete")
            .isFalse();
    }

    private EhrExtractStatus.GpcDocument getUnfinishedGpcDocument() {
        return getGpcDocument(null);
    }

    private EhrExtractStatus.GpcAccessStructured getUnfinishedGpcAccessStructured() {
        return getGpcAccessStructured(null);
    }

    @Test
    void When_DocumentAccessStepIsNotFinished_Expect_ReturnFalse() {
        var ehrExtractStatus = buildEhrExtractStatus(List.of(getUnfinishedGpcDocument(), getUnfinishedGpcDocument()),
            getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("document access is incomplete")
            .isFalse();
    }

    @Test
    void When_OnlyOneDocumentAccessStepIsFinished_Expect_ReturnFalse() {
        var ehrExtractStatus = buildEhrExtractStatus(List.of(getUnfinishedGpcDocument(), getFinishedGpcDocument()),
            getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("all documents must be ready")
            .isFalse();
    }

    @Test
    void When_AccessStructuredStepIsNotFinished_Expect_ReturnFalse() {
        var ehrExtractStatus = buildEhrExtractStatus(List.of(getFinishedGpcDocument(), getFinishedGpcDocument()),
            getUnfinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("structured access is incomplete")
            .isFalse();
    }

    @Test
    void When_AllPreparingDataStepsWereNotStarted_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("no steps started")
            .isFalse();
    }

    @Test
    void When_AccessStructuredStepIsNotStarted_Expect_ReturnFalse() {
        var ehrExtractStatus = buildEhrExtractStatusWithDocumentsOnly(List.of(getFinishedGpcDocument(), getFinishedGpcDocument()));

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("structured step must be present")
            .isFalse();
    }

    @Test
    void When_AccessDocumentStepIsNotStarted_Expect_ReturnFalse() {
        var ehrExtractStatus = buildEhrExtractStatusWithStructuredOnly(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus))
            .as("document access step must be present")
            .isFalse();
    }

    @Test
    void When_AllDocumentsInEhrExtractStatusAreSent_Expect_True() {
        var ehrExtractStatus = buildEhrExtractStatusWithDocumentsOnly(List.of(getFinishedGpcDocumentSent()));

        assertThat(EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus))
            .as("single document was sent to MHS")
            .isTrue();
    }

    private EhrExtractStatus.GpcDocument getFinishedGpcDocumentSent() {
        var doc = getFinishedGpcDocument();
        doc.setSentToMhs(new EhrExtractStatus.GpcAccessDocument.SentToMhs(List.of("123"), "123", "123"));
        return doc;
    }

    @Test
    void When_OneDocumentIsSentAndOneDocumentNotSent_Expect_False() {
        var ehrExtractStatus = buildEhrExtractStatusWithDocumentsOnly(
            List.of(getFinishedGpcDocumentSent(), getFinishedGpcDocument()));

        assertThat(EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus))
            .as("all documents must be sent")
            .isFalse();
    }

    @Test
    void When_DocumentsExistButNoneAreSent_Expect_False() {
        var ehrExtractStatus = buildEhrExtractStatusWithDocumentsOnly(List.of(getFinishedGpcDocument()));

        assertThat(EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus))
            .as("documents without sent-to-MHS marker are not considered sent")
            .isFalse();
    }

    private EhrExtractStatus buildEhrExtractStatus(List<EhrExtractStatus.GpcDocument> documents,
                                                    EhrExtractStatus.GpcAccessStructured gpcAccessStructured) {
        var ehrExtractStatus = new EhrExtractStatus();
        ehrExtractStatus.setGpcAccessDocument(new EhrExtractStatus.GpcAccessDocument(documents, PATIENT_ID));
        ehrExtractStatus.setGpcAccessStructured(gpcAccessStructured);
        return ehrExtractStatus;
    }

    private EhrExtractStatus buildEhrExtractStatusWithDocumentsOnly(List<EhrExtractStatus.GpcDocument> documents) {
        var ehrExtractStatus = new EhrExtractStatus();
        ehrExtractStatus.setGpcAccessDocument(new EhrExtractStatus.GpcAccessDocument(documents, PATIENT_ID));
        return ehrExtractStatus;
    }

    private EhrExtractStatus buildEhrExtractStatusWithStructuredOnly(EhrExtractStatus.GpcAccessStructured gpcAccessStructured) {
        var ehrExtractStatus = new EhrExtractStatus();
        ehrExtractStatus.setGpcAccessStructured(gpcAccessStructured);
        return ehrExtractStatus;
    }
}
