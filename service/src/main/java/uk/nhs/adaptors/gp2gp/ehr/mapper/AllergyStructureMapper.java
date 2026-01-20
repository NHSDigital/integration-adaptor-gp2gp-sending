package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceClinicalStatus;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor.extractOnsetDate;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor.extractAssertedDate;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AllergyStructureTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AllergyStructureMapper {
    private static final Mustache ALLERGY_STRUCTURE_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_allergy_structure_template.mustache");
    private static final String UNSPECIFIED_ALLERGY_CODE =
        "<code code=\"SN53.00\" codeSystem=\"2.16.840.1.113883.2.1.6.2\" displayName=\"Allergy, unspecified\"/>";
    private static final String DRUG_ALLERGY_CODE =
        "<code code=\"14L..00\" codeSystem=\"2.16.840.1.113883.2.1.6.2\" displayName=\"H/O: drug allergy\"/>";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;
    private final ConfidentialityService confidentialityService;
    private final PertinentInformationAllergyMapper pertinentInformationAllergyMapper;

    public String mapAllergyIntoleranceToAllergyStructure(AllergyIntolerance allergyIntolerance) {
        final var idMapper = messageContext.getIdMapper();
        final var allergyStructureId = idMapper.getOrNew(ResourceType.AllergyIntolerance, allergyIntolerance.getIdElement());
        final var categoryCode = buildCategoryCode(allergyIntolerance);
        final var effectiveTime = buildEffectiveTime(allergyIntolerance);
        final var availabilityTime = buildAvailabilityTime(allergyIntolerance);
        final var confidentialityCode = confidentialityService.generateConfidentialityCode(allergyIntolerance).orElse(null);
        final var observationId = idMapper.getOrNew(ResourceType.Observation, allergyIntolerance.getIdElement());
        final var code = buildCode(allergyIntolerance);
        final var pertinentInformation = pertinentInformationAllergyMapper.buildPertinentInformation(allergyIntolerance);
        final var author = buildAuthor(allergyIntolerance).orElse(null);
        final var performer = buildPerformer(allergyIntolerance).orElse(null);

        var allergyStructureTemplateParameters = AllergyStructureTemplateParameters.builder()
            .allergyStructureId(allergyStructureId)
            .categoryCode(categoryCode)
            .effectiveTime(effectiveTime)
            .availabilityTime(availabilityTime)
            .confidentialityCode(confidentialityCode)
            .observationId(observationId)
            .code(code)
            .pertinentInformation(pertinentInformation)
            .author(author)
            .performer(performer);

        return TemplateUtils.fillTemplate(ALLERGY_STRUCTURE_TEMPLATE, allergyStructureTemplateParameters.build());
    }

    private String buildCode(AllergyIntolerance allergyIntolerance) {
        if (!allergyIntolerance.hasClinicalStatus() || !allergyIntolerance.hasCode()) {
            throw new EhrMapperException("Allergy code not present");
        }
        final var clinicalStatus = allergyIntolerance.getClinicalStatus();
        final var allergyCode = allergyIntolerance.getCode();

        if (AllergyIntoleranceClinicalStatus.RESOLVED.equals(clinicalStatus)) {
            var category = getAllergyCategory(allergyIntolerance);

            if (category.equals(AllergyIntoleranceCategory.ENVIRONMENT)) {
                return codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(allergyCode, clinicalStatus);
            }
            if (category.equals(AllergyIntoleranceCategory.MEDICATION)) {
                return codeableConceptCdMapper.mapToNullFlavorCodeableConceptForAllergy(allergyCode, clinicalStatus);
            }
            throw new EhrMapperException("Category could not be mapped");
        }
        return codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(allergyCode, clinicalStatus);
    }

    private String buildAvailabilityTime(AllergyIntolerance allergyIntolerance) {
        var availabilityTime = extractAssertedDate(allergyIntolerance);
        return StatementTimeMappingUtils.prepareAvailabilityTimeForAllergyIntolerance(availabilityTime);
    }

    private String buildEffectiveTime(AllergyIntolerance allergyIntolerance) {
        var onsetDate = extractOnsetDate(allergyIntolerance);
        return StatementTimeMappingUtils.prepareEffectiveTimeForAllergyIntolerance(onsetDate);
    }

    private String buildCategoryCode(AllergyIntolerance allergyIntolerance) {
        var category = getAllergyCategory(allergyIntolerance);

        if (category.equals(AllergyIntoleranceCategory.ENVIRONMENT)) {
            return UNSPECIFIED_ALLERGY_CODE;
        }
        if (category.equals(AllergyIntoleranceCategory.MEDICATION)) {
            return DRUG_ALLERGY_CODE;
        }
        throw new EhrMapperException("Category could not be mapped");
    }

    private Optional<String> buildAuthor(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasRecorder()
            ? buildParticipant(allergyIntolerance.getRecorder(), ParticipantType.AUTHOR)
            : Optional.empty();
    }
    private Optional<String> buildParticipant(Reference reference, ParticipantType participantType) {
        var resourceType = reference.getReferenceElement().getResourceType();
        if (!resourceType.startsWith(ResourceType.Practitioner.name())) {
            return Optional.empty();
        }
        var authorReferenceId = messageContext.getAgentDirectory().getAgentId(reference);
        return Optional.of(participantMapper.mapToParticipant(authorReferenceId, participantType));
    }

    private Optional<String> buildPerformer(AllergyIntolerance allergyIntolerance) {
        if (isValidAsserter(allergyIntolerance)) {
            return buildParticipant(allergyIntolerance.getAsserter(), ParticipantType.PERFORMER);
        }
        if (allergyIntolerance.hasRecorder()) {
            return buildParticipant(allergyIntolerance.getRecorder(), ParticipantType.PERFORMER);
        }
        return Optional.empty();
    }

    private AllergyIntoleranceCategory getAllergyCategory(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.getCategory()
            .stream()
            .map(Enumeration::getValue)
            .filter(this::isMedicationOrEnvironmentCategory)
            .findFirst()
            .orElse(null);
    }

    private boolean isValidAsserter(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasAsserter()
            && allergyIntolerance.getAsserter().getReferenceElement().getResourceType().startsWith(ResourceType.Practitioner.name());
    }

    private boolean isMedicationOrEnvironmentCategory(AllergyIntoleranceCategory category) {
        return category.equals(AllergyIntoleranceCategory.ENVIRONMENT)
            || category.equals(AllergyIntoleranceCategory.MEDICATION);
    }
}
