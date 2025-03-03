package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SpecimenCompoundStatementTemplateParameters {
    private String compoundStatementId;
    private String availabilityTimeElement;
    private String specimenRoleId;
    private String accessionIdentifier;
    private String effectiveTime;
    private String specimenMaterialType;
    private String narrativeStatement;
    private String narrativeStatementId;
    private String participant;
    private String observations;
    private String confidentialityCode;
}
