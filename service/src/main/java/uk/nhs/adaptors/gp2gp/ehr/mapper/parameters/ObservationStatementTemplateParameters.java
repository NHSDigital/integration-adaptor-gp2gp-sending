package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ObservationStatementTemplateParameters {
    private String observationStatementId;
    private String comment;
    private String effectiveTime;
    private String confidentialityCode;
    private String issued;
    private String value;
    private String referenceRange;
    private String participant;
    private boolean isNested;
    private String code;
    private String interpretation;
}
