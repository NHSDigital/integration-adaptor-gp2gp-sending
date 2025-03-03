package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.dstu3.model.Coding;

import java.util.List;

@Getter
@Setter
@Builder
public class CodeableConceptCdTemplateParameters {
    private String mainCode;
    private String mainCodeSystem;
    private String mainDisplayName;
    private String mainOriginalText;
    private List<Coding> translations;
    private boolean nullFlavor;
}
