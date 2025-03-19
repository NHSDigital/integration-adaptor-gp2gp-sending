package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.CodeableConceptCdTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeSystemsUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CodeableConceptCdMapper {

    private static final Mustache CODEABLE_CONCEPT_CD_TEMPLATE = TemplateUtils
        .loadTemplate("codeable_concept_cd_template.mustache");
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String CARE_CONNECT_PRESCRIBING_AGENCY_SYSTEM = "https://fhir.nhs.uk/STU3/CodeSystem/CareConnect-PrescribingAgency-1";
    private static final String SNOMED_SYSTEM_CODE = "2.16.840.1.113883.2.1.3.2.4.15";
    private static final String DESCRIPTION_ID = "descriptionId";
    private static final String DESCRIPTION_DISPLAY = "descriptionDisplay";
    private static final String DESCRIPTION_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-coding-sctdescid";
    private static final String FIXED_ACTUAL_PROBLEM_CODE = "55607006";
    private static final String PROBLEM_DISPLAY_NAME = "Problem";
    private static final String ACTIVE_CLINICAL_STATUS = "active";
    private static final String PRESCRIBING_AGENCY_GP_PRACTICE_CODE = "prescribed-at-gp-practice";
    private static final String PRESCRIBING_AGENCY_PREVIOUS_PRACTICE_CODE = "prescribed-by-previous-practice";
    private static final String PRESCRIBING_AGENCY_ANOTHER_ORGANISATION_CODE = "prescribed-by-another-organisation";
    private static final String EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_CODE = "394823007";
    private static final String EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_DISPLAY = "NHS Prescription";
    private static final String EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_CODE = "394828003";
    private static final String EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_DISPLAY = "Prescription by another organisation";
    private static final String UNSPECIFIED_PROBLEM_CODE = "394776006";
    private static final String UNSPECIFIED_PROBLEM_DESCRIPTION = "Unspecified problem";
    private static final String OTHER_CATEGORY_CODE = "394841004";
    private static final String OTHER_CATEGORY_DESCRIPTION = "Other category";

    public String mapCodeableConceptToCd(CodeableConcept codeableConcept) {
        return mapCodeableConcept(codeableConcept, this::getMainCode);
    }

    public String mapCodeableConceptForMedication(CodeableConcept codeableConcept) {
        return mapCodeableConcept(
            codeableConcept,
            (descriptionExtensions, snomedCodeCoding) -> Optional.ofNullable(snomedCodeCoding.getCode()));
    }

    private String mapCodeableConcept(
        CodeableConcept codeableConcept,
        BiFunction<Optional<List<Extension>>, Coding, Optional<String>> getMainCodeFunction) {
        Optional<Coding> snomedCodeCoding = getSnomedCodeCoding(codeableConcept);

        if (snomedCodeCoding.isEmpty()) {
            return mapToNullFlavorCodeableConcept(codeableConcept);
        }

        var builder = CodeableConceptCdTemplateParameters.builder();

        var descriptionExtensions = retrieveDescriptionExtension(snomedCodeCoding.get())
            .map(Extension::getExtension);

        builder.mainCodeSystem(SNOMED_SYSTEM_CODE);

        var mainDisplayName = getMainDisplayName(descriptionExtensions, snomedCodeCoding.get());
        mainDisplayName.ifPresent(builder::mainDisplayName);

        builder.mainOriginalText(codeableConcept.getText());
        builder.translations(getNonSnomedCodeCodings(codeableConcept));

        var mainCode = getMainCodeFunction.apply(descriptionExtensions, snomedCodeCoding.get());
        mainCode.ifPresent(builder::mainCode);

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    public String mapCodeableConceptToCdForAllergy(
        CodeableConcept codeableConcept,
        AllergyIntolerance.AllergyIntoleranceClinicalStatus allergyIntoleranceClinicalStatus
    ) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        Optional<Coding> snomedCodeCoding = getSnomedCodeCoding(codeableConcept);

        if (snomedCodeCoding.isEmpty()) {
            builder.nullFlavor(true);
            return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
        }

        if (ACTIVE_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
            builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
            builder.translations(getNonSnomedCodeCodings(codeableConcept));
        } else {
            builder.nullFlavor(true);
        }

        getAllergyMainCode(snomedCodeCoding.get()).ifPresent(builder::mainCode);
        getCodingDisplayName(snomedCodeCoding.get()).ifPresent(builder::mainDisplayName);

        if (codeableConcept.hasText()) {
            builder.mainOriginalText(codeableConcept.getText());
        } else {
            var originalText = findOriginalTextForAllergy(codeableConcept, snomedCodeCoding, allergyIntoleranceClinicalStatus);
            originalText.ifPresent(builder::mainOriginalText);
        }

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    private static Optional<String> getCodingDisplayName(Coding snomedCodeCoding) {
        return Optional.ofNullable(snomedCodeCoding.getDisplay());
    }

    private static Optional<String> getAllergyMainCode(Coding snomedCodeCoding) {
        return retrieveDescriptionExtension(snomedCodeCoding)
            .flatMap(extension -> extension.getExtension().stream()
                .filter(descriptionExt -> DESCRIPTION_ID.equals(descriptionExt.getUrl()))
                .findFirst()
                .map(description -> description.getValue().toString()))
            .or(() -> Optional.ofNullable(snomedCodeCoding.getCode()));
    }

    public String mapCodeableConceptToCdForTransformedActualProblemHeader(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();

        var originalText = CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
        originalText.ifPresent(builder::mainOriginalText);

        builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
        builder.mainCode(FIXED_ACTUAL_PROBLEM_CODE);
        builder.mainDisplayName(PROBLEM_DISPLAY_NAME);

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    public String mapCodeableConceptToCdForBloodPressure(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var mainCode = getSnomedCodeCoding(codeableConcept);

        builder.nullFlavor(mainCode.isEmpty());
        var originalText = findOriginalText(codeableConcept, mainCode);
        originalText.ifPresent(builder::mainOriginalText);

        if (mainCode.isPresent()) {
            builder.mainCodeSystem(SNOMED_SYSTEM_CODE);
            var code = Optional.ofNullable(mainCode.get().getCode());
            var displayText = findDisplayText(mainCode.get());

            code.ifPresent(builder::mainCode);
            displayText.ifPresent(builder::mainDisplayName);
            builder.translations(getNonSnomedCodeCodings(codeableConcept));
        }

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    public Optional<String> mapCodeableConceptToCdForEhrSupplyType(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder();
        var prescribingAgency = findPrescribingAgency(codeableConcept);
        String code;
        String displayText;

        if (prescribingAgency.isEmpty()) {
            return Optional.empty();
        }

        switch (prescribingAgency.orElseThrow().getCode()) {
            case PRESCRIBING_AGENCY_GP_PRACTICE_CODE:
            case PRESCRIBING_AGENCY_PREVIOUS_PRACTICE_CODE:
                code = EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_CODE;
                displayText = EHR_SUPPLY_TYPE_NHS_PRESCRIPTION_DISPLAY;
                break;
            case PRESCRIBING_AGENCY_ANOTHER_ORGANISATION_CODE:
                code = EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_CODE;
                displayText = EHR_SUPPLY_TYPE_ANOTHER_ORGANISATION_DISPLAY;
                break;
            default:
                return Optional.empty();
        }

        builder
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainCode(code)
            .mainDisplayName(displayText);

        return Optional.of(TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build()));
    }

    public String getCdForTopic() {
        var params = CodeableConceptCdTemplateParameters.builder()
            .mainCode(UNSPECIFIED_PROBLEM_CODE)
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainDisplayName(UNSPECIFIED_PROBLEM_DESCRIPTION)
            .build();

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, params);
    }

    public String mapToCdForTopic(CodeableConcept relatedProblem, String title) {
        var mainCode = getSnomedCodeCoding(relatedProblem);

        if (mainCode.isEmpty()) {
            return mapToCdForTopic(title);
        }

        var params = prepareTemplateParameters(mainCode.orElseThrow());
        params.setMainOriginalText(title);

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, params);
    }

    public String mapToCdForTopic(CodeableConcept relatedProblem) {
        var mainCode = getSnomedCodeCoding(relatedProblem);

        if (mainCode.isEmpty()) {
            return getCdForTopic();
        }

        var params = prepareTemplateParameters(mainCode.orElseThrow());

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, params);
    }

    public String mapToCdForTopic(String title) {
        var params = CodeableConceptCdTemplateParameters.builder()
            .mainCode(UNSPECIFIED_PROBLEM_CODE)
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainDisplayName(UNSPECIFIED_PROBLEM_DESCRIPTION)
            .mainOriginalText(title)
            .build();

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, params);

    }

    public String mapToCdForCategory(String title) {
        var params = CodeableConceptCdTemplateParameters.builder()
            .mainCode(OTHER_CATEGORY_CODE)
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainDisplayName(OTHER_CATEGORY_DESCRIPTION)
            .mainOriginalText(title)
            .build();

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, params);
    }

    public String getCdForCategory() {
        var params = CodeableConceptCdTemplateParameters.builder()
            .mainCode(OTHER_CATEGORY_CODE)
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainDisplayName(OTHER_CATEGORY_DESCRIPTION)
            .build();

        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, params);
    }

    private CodeableConceptCdTemplateParameters prepareTemplateParameters(Coding mainCode) {
        var extensions = retrieveDescriptionExtension(mainCode)
            .map(Extension::getExtension)
            .orElse(Collections.emptyList());

        Optional<String> displayCode = findDescriptionExtValue(extensions);
        Optional<String> displayName = findDisplayExtValue(extensions);

        return CodeableConceptCdTemplateParameters.builder()
            .mainCodeSystem(SNOMED_SYSTEM_CODE)
            .mainCode(displayCode.orElse(mainCode.getCode()))
            .mainDisplayName(displayName.orElse(mainCode.getDisplay()))
            .build();
    }

    private Optional<String> findDescriptionExtValue(List<Extension> extensions) {
        return extensions.stream()
            .filter(ext -> ext.getUrl().equals(DESCRIPTION_ID))
            .map(descriptionExt -> descriptionExt.getValue().toString())
            .findFirst();
    }

    private Optional<String> findDisplayExtValue(List<Extension> extensions) {
        return extensions.stream()
            .filter(ext -> ext.getUrl().equals(DESCRIPTION_DISPLAY))
            .map(displayExt -> displayExt.getValue().toString())
            .findFirst();
    }

    private Optional<Coding> findPrescribingAgency(CodeableConcept codeableConcept) {
        return codeableConcept.getCoding()
            .stream()
            .filter(this::isPrescribingAgency)
            .findFirst();
    }

    private Optional<Coding> getSnomedCodeCoding(CodeableConcept codeableConcept) {
        return codeableConcept.getCoding()
            .stream()
            .filter(this::isSnomed)
            .findFirst();
    }

    private List<Coding> getNonSnomedCodeCodings(CodeableConcept codeableConcept) {
        var nonSnomedCodeCodings = codeableConcept.getCoding()
            .stream()
            .filter(coding -> !isSnomed(coding))
            .filter(coding -> !CodeSystemsUtil.getHl7code(coding.getSystem()).isEmpty())
            .toList();

        for (Coding coding : nonSnomedCodeCodings) {
            var hl7CodeSystem = CodeSystemsUtil.getHl7code(coding.getSystem());
            coding.setSystem(hl7CodeSystem);
        }

        return nonSnomedCodeCodings;
    }

    private Optional<String> findOriginalText(CodeableConcept codeableConcept, Optional<Coding> coding) {

        if (codeableConcept.hasText()) {
            return Optional.ofNullable(codeableConcept.getText());
        }

        if (coding.isPresent()) {
            if (coding.get().hasDisplay()) {
                return getCodingDisplayName(coding.get());
            }

            return getDisplayTextFromDescriptionExtension(coding.get());
        }

        return CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
    }

    private Optional<String> findOriginalTextForAllergy(
        CodeableConcept codeableConcept,
        Optional<Coding> coding,
        AllergyIntolerance.AllergyIntoleranceClinicalStatus allergyIntoleranceClinicalStatus
    ) {
        if (coding.isEmpty()) {
            return Optional.empty();
        }

        if (ACTIVE_CLINICAL_STATUS.equals(allergyIntoleranceClinicalStatus.toCode())) {
            return getDisplayTextFromDescriptionExtension(coding.get());
        }

        return CodeableConceptMappingUtils.extractTextOrCoding(codeableConcept);
    }

    private Optional<String> getDisplayTextFromDescriptionExtension(Coding coding) {
        return retrieveDescriptionExtension(coding)
            .flatMap(value -> value
                .getExtension().stream()
                .filter(displayExtension -> DESCRIPTION_DISPLAY.equals(displayExtension.getUrl()))
                .map(extension1 -> extension1.getValue().toString())
                .findFirst()
            );
    }

    private Optional<String> findDisplayText(Coding coding) {
        return Optional.ofNullable(coding.getDisplay());
    }

    private boolean isSnomed(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM);
    }

    private boolean isPrescribingAgency(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(CARE_CONNECT_PRESCRIBING_AGENCY_SYSTEM);
    }

    private static Optional<Extension> retrieveDescriptionExtension(Coding coding) {
        return coding
            .getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(DESCRIPTION_URL))
            .findFirst();
    }

    public String getDisplayFromCodeableConcept(CodeableConcept codeableConcept) {
        return getSnomedCodeCoding(codeableConcept)
            .map(cc -> findDisplayText(cc).orElse(StringUtils.EMPTY))
            .orElse(StringUtils.EMPTY);
    }

    public String mapToNullFlavorCodeableConcept(CodeableConcept codeableConcept) {
        var builder = CodeableConceptCdTemplateParameters.builder().nullFlavor(true);
        var mainCode = getSnomedCodeCoding(codeableConcept);

        var originalText = findOriginalText(codeableConcept, mainCode);
        originalText.ifPresent(builder::mainOriginalText);
        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    public String mapToNullFlavorCodeableConceptForAllergy(
        CodeableConcept codeableConcept,
        AllergyIntolerance.AllergyIntoleranceClinicalStatus allergyIntoleranceClinicalStatus
    ) {
        var builder = CodeableConceptCdTemplateParameters.builder().nullFlavor(true);
        var mainCode = getSnomedCodeCoding(codeableConcept);

        var originalText = findOriginalTextForAllergy(codeableConcept, mainCode, allergyIntoleranceClinicalStatus);
        originalText.ifPresent(builder::mainOriginalText);
        return TemplateUtils.fillTemplate(CODEABLE_CONCEPT_CD_TEMPLATE, builder.build());
    }

    private Optional<String> getMainCode(Optional<List<Extension>> descriptionExtensions, Coding snomedCodeCoding) {
        if (descriptionExtensions.isPresent()) {
            var descriptionCode = descriptionExtensions.get().stream()
                .filter(descriptionExt -> DESCRIPTION_ID.equals(descriptionExt.getUrl()))
                .map(description -> description.getValue().toString())
                .findFirst();

            if (descriptionCode.isPresent()) {
                return descriptionCode;
            }
        }

        return Optional.ofNullable(snomedCodeCoding.getCode());
    }

    private Optional<String> getMainDisplayName(Optional<List<Extension>> descriptionExtensions, Coding snomedCodeCoding) {
        if (descriptionExtensions.isPresent()) {
            var descriptionDisplayName = descriptionExtensions.get().stream()
                .filter(descriptionExt -> DESCRIPTION_DISPLAY.equals(descriptionExt.getUrl()))
                .map(description -> description.getValue().toString())
                .findFirst();

            if (descriptionDisplayName.isPresent()) {
                return descriptionDisplayName;
            }
        }

        return Optional.ofNullable(snomedCodeCoding.getDisplay());
    }
}