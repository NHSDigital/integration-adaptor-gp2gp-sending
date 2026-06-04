package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor.extractReaction;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toTextFormat;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class PertinentInformationAllergyMapper {
    private static final String ALLERGY_INTOLERANCE_END_URL =
        "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-AllergyIntoleranceEnd-1";
    private static final String END_DATE = "End Date: ";
    private static final String STATUS = "Status: ";
    private static final String TYPE = "Type: ";
    private static final String CRITICALITY = "Criticality: ";
    private static final String PATIENT_ASSERTER = "Asserted By Patient";
    private static final String RELATED_PERSON_ASSERTER = "Asserted By: Related Person";
    private static final String LAST_OCCURRENCE = "Last Occurred: ";
    private static final String PATIENT_RECORDER = "Recorded By Patient";
    private static final String COMMA = ", ";


    private final MessageContext messageContext;

    public String buildPertinentInformation(AllergyIntolerance allergyIntolerance) {
        return retrievePertinentInformation(allergyIntolerance)
            .stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrievePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return List.of(
            buildExtensionReasonEndPertinentInformation(allergyIntolerance),
            buildClinicalStatusPertinentInformation(allergyIntolerance),
            buildTypePertinentInformation(allergyIntolerance),
            buildCriticalityPertinentInformation(allergyIntolerance),
            buildAsserterPertinentInformation(allergyIntolerance),
            buildLastOccurrencePertinentInformation(allergyIntolerance),
            buildRecorderPertinentInformation(allergyIntolerance),
            buildReactionPertinentInformation(allergyIntolerance),
            buildNotePertinentInformation(allergyIntolerance),
            buildEndDatePertinentInformation(allergyIntolerance)
        );
    }

    private String buildExtensionReasonEndPertinentInformation(AllergyIntolerance allergyIntolerance) {
        return filterExtensionByUrl(allergyIntolerance, ALLERGY_INTOLERANCE_END_URL)
            .map(AllergyStructureExtractor::extractReasonEnd)
            .orElse(StringUtils.EMPTY);
    }

    private String buildClinicalStatusPertinentInformation(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasClinicalStatus()
            ? buildClinicalStatusNotePertinentInformation(allergyIntolerance)
            : StringUtils.EMPTY;
    }

    private String buildClinicalStatusNotePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasNote()
            ? STATUS + StringUtils.capitalize(allergyIntolerance.getClinicalStatus().toCode())
            : STATUS + allergyIntolerance.getClinicalStatus().getDisplay();
    }

    private String buildTypePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasType()
            ? TYPE + allergyIntolerance.getType().getDisplay()
            : StringUtils.EMPTY;
    }

    private String buildCriticalityPertinentInformation(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasCriticality()
            ? CRITICALITY + allergyIntolerance.getCriticality().getDisplay()
            : StringUtils.EMPTY;
    }

    private String buildAsserterPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasAsserter()) {
            IIdType reference = allergyIntolerance.getAsserter().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return PATIENT_ASSERTER;
            } else if (reference.getResourceType().equals(ResourceType.RelatedPerson.name())) {
                return messageContext
                    .getInputBundleHolder()
                    .getResource(reference)
                    .map(RelatedPerson.class::cast)
                    .filter(RelatedPerson::hasName)
                    .map(RelatedPerson::getName)
                    .filter(names -> !names.isEmpty())
                    .map(List::getFirst)
                    .map(name -> Optional.ofNullable(name.getText())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(name::getNameAsSingleString))
                    .filter(StringUtils::isNotBlank)
                    .map(name -> RELATED_PERSON_ASSERTER + StringUtils.SPACE + name)
                    .orElse(RELATED_PERSON_ASSERTER);
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildLastOccurrencePertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasLastOccurrence()) {
            return LAST_OCCURRENCE + toTextFormat(allergyIntolerance.getLastOccurrenceElement());
        }
        return StringUtils.EMPTY;
    }

    private String buildRecorderPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasRecorder()) {
            IIdType reference = allergyIntolerance.getRecorder().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return PATIENT_RECORDER;
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildReactionPertinentInformation(AllergyIntolerance allergyIntolerance) {
        AtomicInteger reactionCount = new AtomicInteger(1);
        if (allergyIntolerance.hasReaction()) {
            return allergyIntolerance.getReaction()
                .stream()
                .map(reaction -> extractReaction(reaction, reactionCount))
                .collect(Collectors.joining(COMMA));
        }
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return Stream.concat(
                messageContext.getInputBundleHolder().getRelatedConditions(allergyIntolerance.getId())
                    .stream()
                    .map(Condition::getNote)
                    .flatMap(List::stream),
                allergyIntolerance.hasNote() ? allergyIntolerance.getNote().stream() : Stream.empty()
            )
            .map(Annotation::getText)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String buildEndDatePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return filterExtensionByUrl(allergyIntolerance, ALLERGY_INTOLERANCE_END_URL)
            .map(extension -> AllergyStructureExtractor.extractEndDate(extension, DateFormatUtil::toTextFormat))
            .filter(StringUtils::isNotBlank)
            .map(endDate -> END_DATE + endDate)
            .orElse(StringUtils.EMPTY);
    }
}
