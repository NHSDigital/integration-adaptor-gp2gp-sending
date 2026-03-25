# WireMock

## Patients - `migratestructuredrecord` endpoint

Each scenario below can be retrieved by requesting the associated NHS Number specified.

### Invalid/Error Responses
- [OperationOutcome - Invalid NHS Number](stubs/__files/operationOutcomeInvalidNHSNumber.json) 123456789, 960000001
- [OperationOutcome - Bad Request](stubs/__files/operationOutcomeBadRequest.json) 9600000005
- [OperationOutcome - Internal Server Error](stubs/__files/operationOutcomeInternalServerError.json) 9600000006
- [OperationOutcome - Invalid Demographics](stubs/__files/operationOutcomeInvalidDemographic.json) 9600000002
- [OperationOutcome - Invalid Parameter](stubs/__files/operationOutcomeInvalidParameter.json) 9600000004
- [OperationOutcome - Invalid Resource](stubs/__files/operationOutcomeInvalidResource.json) 9600000003
- [OperationOutcome - No Relationship](stubs/__files/operationOutcomeNoRelationship.json) 9600000010
- [Bundle without Patient Resource](stubs/__files/malformedStructuredRecordMissingPatientResource.json) 2906543841
- [OperationOutcome - Not Found](stubs/__files/operationOutcomePatientNotFound.json) 9600000009

### Valid Bundles

- [Allergies](stubs/__files/correctAllergiesContainedResourceResponse.json) 9728951256
- [BundleFromMedicus](stubs/__files/MedicusBasedOnErrorStructuredRecord.json) 9302014592
- [Observation with bodySite property](stubs/__files/correctPatientStructuredRecordResponseBodySite.json) 1239577290
- [Large (3Mb) structured record Bundle](stubs/__files/correctPatientStructuredRecordLargePayload.json) 9690937421
- [Malformed date](stubs/__files/malformedDateStructuredRecord.json) 9690872294
- ["Normal"](stubs/__files/correctPatientStructuredRecordResponseNormal.json) 9690937287

#### Document Scenarios

- [No Documents](stubs/__files/correctPatientNoDocsStructuredRecordResponse.json) 9690937294
- [1 Absent Attachment](stubs/__files/correctPatientStructuredRecordResponseAbsentAttachment.json) 9690937286
- [3 Absent Attachments](stubs/__files/correctPatientStructuredRecordResponse3AbsentAttachmentDocuments.json) 9690937419
- [3 Attachments with 2 Absent](stubs/__files/correctPatientStructuredRecordResponse3AttachmentsWith2Absent.json) 9690939911
- [With three 10Kb .doc files](stubs/__files/correctPatientStructuredRecordResponse3NormalDocuments.json) 9690937420
- [With one 20Kb document](stubs/__files/correctPatientStructuredRecordResponseForLargeDocs.json) 9690937819
- [With one 40Kb document](stubs/__files/correctPatientStructuredRecordResponseForLargeDocs2.json) 9690937841
- [With one 74Kb docx file](stubs/__files/correctPatientStructuredRecordWithLargeDocxAttachment.json) 9388098434
- [One 10Kb doc, one 4.703Kb doc](stubs/__files/correctPatientStructuredRecordResponseOneLargeDocOneNormal.json) 9690937789
- [With invalid content type document](stubs/__files/correctPatientStructuredRecordResponseOneInvalidContentTypeAttachment.json) 9817280691

### Assurance Patients

- PWTP2 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP2.json) 9726908671 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP2.json) 9726908787
- PWTP3 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP3.json) 9726908698 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP3.json) 9726908795
- PWTP4 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP4.json) 9726908701 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP4.json) 9726908809
- PWTP5 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP5.json) 9726908728 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP5.json) 9726908817
- PWTP6 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP6.json) 9726908736 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP6.json) 9726908825
- PWTP7 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP7.json) 9726908744 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP7.json) 9726908833
- PWTP9 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP9.json) 9726908752 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP9.json) 9726908841
- PWTP10 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP10.json) 9726908760 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP10.json) 9726908868
- PWTP11 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP11.json) 9726908779 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP11.json) 9726908876


## Patients - `migratestructuredrecord` endpoint, unknown patient

The `migratestructuredrecord` endpoint will also respond to unknown NHS Numbers.
By default, it returns a NOT FOUND response, but it can also be configured to reply with
a valid patient record.

WireMock can also be configured to fetch patient information (name, DOB, address)
from the PDS Application-Restricted FHIR API.

Using the [authentication guidance], you will need to provide the following environment variables:

- `PDS_KEY_ID` - "Key Identifier" from Step 2
- `PDS_API_KEY` - "API Key" from Step 1
- `PDS_PRIVATE_KEY` - "Private key" from Step 2, as a string without whitespace, and the `---` header and footers removed.

[authentication guidance]: https://digital.nhs.uk/developer/guides-and-documentation/security-and-authorisation/application-restricted-restful-apis-signed-jwt-authentication

### Changing the Default Record

To change the patient record returned to be [Internal Server Error](stubs/__files/operationOutcomeInternalServerError.json):

```shell
curl --request PUT --data '{"state": "Internal Server Error"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to be [PWTP3, which has many Prescriptions](stubs/__files/EMISPatientStructurede2eResponsePWTP3.json):

```shell
curl --request PUT --data '{"state": "Medications"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to be [PWTP6, which has many Immunisations](stubs/__files/EMISPatientStructurede2eResponsePWTP6.json):

```shell
curl --request PUT --data '{"state": "Immunisations"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to be [PWTP9, which has many Investigations](stubs/__files/EMISPatientStructurede2eResponsePWTP9.json):

```shell
curl --request PUT --data '{"state": "Investigations"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to be [Large Patient Record](stubs/__files/correctPatientStructuredRecordLargePayload.json):

```shell
curl --request PUT --data '{"state": "Large Patient Record"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to have different Absent Attachment scenarios [Absent Attachments](stubs/__files/correctPatientStructuredRecordResponseAbsentAttachments.json):

```shell
curl --request PUT --data '{"state": "Absent Attachments"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to have 100 × 3.5 MB attachments (350 MB total) [350MB Documents](stubs/__files/correctPatientStructuredRecordResponse350MBDocuments.json):

```shell
curl --request PUT --data '{"state": "350MB Attachments"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to have a 30MB attachment (useful for testing fragments) [30MBDocument](stubs/__files/correctPatientStructuredRecordResponse30MBAttachment.json):

```shell
curl --request PUT --data '{"state": "30MB Attachment"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```

To change the patient record returned to be NOT FOUND:

```shell
curl --request PUT --data '{"state": "Started"}' http://localhost:8110/__admin/scenarios/migrateStructuredRecord/state
```
