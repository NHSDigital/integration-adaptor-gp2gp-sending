# **GP2GP Redactions**

Redaction is the process of restricting access or ‘hiding’ information in the online viewer from the patient and anyone 
they have granted proxy access to.  It does not remove the information from the patient’s record.

Before information is shared, sensitive information which could be harmful to a patient or is about or refers to other
people (third parties) should be assessed, and a decision taken about whether or not to redact it.  

Individual words, sentences, or paragraphs within an entry cannot be redacted.  The entire entry, for instance the
consultation or document must be either shared (visible online) or redacted i.e. made not visible online).

## **Why are redactions used**

When GP records are shared with patients or their representatives (nominated proxy), the GP practice is responsible for
ensuring that only appropriate information is disclosed.  To ensure this happens, information in both the existing record
and any new items should be checked and where necessary, redacted.

Most records will not have content that requires redaction.  For individual requests for full online record access (i.e.
past, historic and current records) it is best practice for all of the records to be checked in advance of being shared.

For more information about redactions please review the NHS England Documentation for redactions:

[*https://www.england.nhs.uk/long-read/redacting-information-for-online-record-access/*](https://www.england.nhs.uk/long-read/redacting-information-for-online-record-access/)


## Enabling redactions support in the GP2GP adaptors

The GP2GP Adaptor needs to be deployed with the necessary configuration for redactions to be enabled.

To enable Redactions, the GP2GP Adaptor should be deployed with the following environment variable set as follows:

***`GP2GP_REDACTIONS_ENABLED: true`***

To disable Redactions, the GP2GP Adaptor should be deployed with the following environment variable set as follows:

***`GP2GP_REDACTIONS_ENABLED: false`***

Note that if redactions are not enabled, the resultant XML be produced with an `interactionId` of `RCMR_IN030000UK06` and
redaction security labels will not be populated.

**This setting should be set to `false` until the incumbent systems have enabled redactions functionality across their
whole estate.  If in any doubt please contact NIA Support.**


## How are redactions identified

When sending a patient record using the GP2GP System, a JSON FHIR Bundle is sent. Certain resources (covered below) can 
be marked as redacted by applying a `NOPAT` security label within the resource metadata. `NOPAT` is a code within the 
*ActCode Code System* and signifies that the information should not not be disclosed to the patient, family or 
caregivers.

This label should be applied to the `meta.security` element with the `system`, `code` and `display` values set exactly as
below:

```json
{
  "meta":{
    "security":[
      {
        "system":"http://hl7.org/fhir/v3/ActCode",
        "code":"NOPAT",
        "display":"no disclosure to patient, family or caregivers without attending provider's authorization"
      }
    ]
  }
}
```

When a patient record is received from an incumbent system using the GP2GP System, an `interactionId` of `RCMR_IN030000UK07`
will be provided. Certain elements within the XML may be marked as redacted by a `confidentialityCode` security label
containing a `code` value of `NOPAT`.

This security label should be applied to element being redacted and should be exactly as below:

```xml
<confidentialityCode code="NOPAT" codeSystem="2.16.840.1.113883.4.642.3.47" displayName="no disclosure to patient, family or caregivers without attending provider's authorization"/>
```

## GP2GP Send Adaptor Redactions

This section details the resource types which can be redacted when using the GP2GP Send Adaptor.  Some of these cover
multiple resources within the JSON Bundle.

**This also includes details of any known issues with the redaction being applied when the patient record is received and
integrated by an incumbent (Optum / TPP).**


### Laboratory Results

Laboratory Results consist of a number of resources which can have the `NOPAT` security label applied.


#### Diagnostic Report

To mark a `DiagnosticReport` as redacted, the `NOPAT` security label should be applied to resource with `resource.resourceType` set to `DiagnosticReport`

This will populate the relevant `CompoundStatement[laboratory reporting] / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

**Both Optum and TPP do not currently display redacted `DiagnosticReport` resources in their respective systems.  This has been reviewed and is not clinically relevant.  This will be raised with both Optum and TPP.**

#### Specimen

To mark a `Specimen` as redacted, the `NOPAT` security label should be applied to resource with `resource.resourceType` set to `Specimen`

This will populate the relevant `CompoundStatement[specimen] / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

**Both Optum and TPP do not currently display redacted `Specimen` resources in their respective systems.  This has been reviewed and is not clinically relevant.  This will be raised with both Optum and TPP.**

#### Observation \- Filing Comment

To mark an `Observation (Filing Comment)` as redacted, the `NOPAT` security label should be applied to resource with `resource.resourceType` set to `Observation` and contain a `resource.code` set to `37331000000100` for `Comment Note`

This will populate the `NarrativeStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

#### Observation \- Test Group Header

To mark an `Observation (Test Group Header)` as redacted, the `NOPAT` security label should be applied to the test group header resource with `resource.resourceType` set to `Observation`.

This will populate the relevant `CompoundStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

#### Observation \- Test Result

To mark an `Observation (Test Result)` as redacted, the `NOPAT` security label should be applied to the test result resource with `resource.resourceType` set to `Observation`.

This will populate the relevant `ObservationStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Allergy Intolerance

To mark a `Drug Allergy` or `Non-Drug Allergy` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `AllergyIntolerance`.

This will populate the relevant `ObservationStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Condition

To mark a `Condition` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `Condition`.

When a `Condition` contains an `extension` which provides a reference to an `actual problem` (example provided below) then the `NOPAT` security label should be applied to `Observation` referenced in the header also.

```json
"extension": [     {
        "url": "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ActualProblem-1",
        "valueReference": {
          "reference": "Observation/F16EAE60-77F0-49F3-A424-C8F57FF02358"
        }
    }
]
```

This will populate the relevant `LinkSet / confidentiality code` in the resultant HL7 XML with the `NOPAT` security label.


### Immunization

To mark an `Immunization` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `Immunization`.

This will populate the relevant `ObservationStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Medication Request

To mark a `MedicationRequest` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `MedicationRequest.`

This will populate the relevant `ObservationStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

**When the patient record is received and integrated into Optum, the system can only hide either:**

1. **All medications associated with a repeat.**
2. **None of the medications associated with a repeat**

**This means that if only one of associated `MedicationRequest[Order]` has a `NOPAT` security label applied, then none will be marked as redacted in Optum.**

**When the patient record is received and integrated into TPP and the `MedicationRequest[Order]` has a NOPAT security label applied, but not the associated `MedicationRequest[Plan]` of an acute medication, then the medication will not be marked as redacted in TPP.**

**These have been identified as being clinically acceptable after being reviewed.**


### Document Reference

There are two possible ways to mark a `DocumentReference` as redacted, when applied to a resource with `resource.resourceType` set to `MedicationRequest`:

1. The `NOPAT` security label is applied to `DocumentReference.meta.security` as usual.
2. The `NOPAT` security label is applied to `DocumentReference.securityLabel`.

Either of these will populate the relevant `NarrativeStatement / reference / referredToExternalDocument / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Procedure Request

To mark a `ProcedureRequest` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `ProcedureRequest.`

This will populate the relevant `PlanStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

**When the patient record is received and integrated into Optum, these diary entries will not be marked as redacted.  This has been reviewed and does not present a clinical risk.**


### Referral Request

To mark a `ReferralRequest` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `ReferralRequest.`

This will populate the relevant `RequestStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Observation

An Observation can contain a variety of data.  In addition to the *laboratory results observations* documented above, the following observation resources can also be redacted.

#### Observation \- Blood Pressure

To mark an `Observation(Blood Pressure)` as redacted, the `NOPAT` security label should be applied to the relevant resource with `resource.resourceType` set to `Observation`.

This will populate the relevant `CompoundStatement/ confidentialityCode` in the `CompoundStatement` containing the `ObservationStatement` resource in the resultant HL7 XML with the `NOPAT` security label.

#### Observation \- Uncategorised

To mark an `Observation(Uncategoried)` as redacted, the `NOPAT` security label should be applied to the relevant resource with `resource.resourceType` set to `Observation.`

This will populate the relevant `ObservationStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Encounter

To mark an `Encounter` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `Encounter.`

This will populate the relevant `ehrComposition / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### Referral Request

To mark a `ReferralRequest` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `ReferralRequest.`

This will populate the relevant `RequestStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.


### List \- Topic

To mark a `List(Topic)` as redacted, the `NOPAT` security label should be applied to a resource with `resource.resourceType` set to `List` and contain a `resource.code` set to `25851000000105` for `Topic (EHR)`.

This will populate the relevant `CompoundStatement / confidentialityCode` in the resultant HL7 XML with the `NOPAT` security label.

**When the patient record is received and integrated into Optum or TPP, these will not be marked as redacted.  This is to be raised with EMIS and TPP as they either do not handle, or do not intend to handle the concept of redacting at a topic level.**