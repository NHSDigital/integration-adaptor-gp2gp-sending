package uk.nhs.adaptors.gp2gp.utils;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Meta;

import java.util.Collections;

public final class ConfidentialityCodeUtility {
    private ConfidentialityCodeUtility() { }
    public static final String NOPAT = "NOPAT";
    public static final String NOSCRUB = "NOSCRUB";
    public static final String NOPAT_HL7_CONFIDENTIALITY_CODE = "<confidentialityCode code=\"NOPAT\" "
        + "codeSystem=\"2.16.840.1.113883.4.642.3.47\" "
        + "displayName=\"no disclosure to patient, family or caregivers without attending provider's authorization\" />";

    public static <R extends DomainResource> String getSecurityCodeFromResource(R resource) {
        return resource.getMeta()
            .getSecurity()
            .getFirst()
            .getCode();
    }

    public static <R extends DomainResource> void appendNopatSecurityToMetaForResource(R resource) {
        final Meta meta = resource.getMeta();
        meta.setSecurity(
            Collections.singletonList(
                getNopatCoding()
            )
        );
    }

    public static <R extends DomainResource> void appendNoscrubSecurityToMetaForResource(R resource) {
        final Meta meta = resource.getMeta();
        meta.setSecurity(
            Collections.singletonList(
                getNoscrubCoding()
            )
        );
    }

    public static boolean doesMetaContainNopat(Meta meta) {
        if (!meta.getSecurity().isEmpty()) {
            final Coding coding = meta.getSecurity().getFirst();
            return coding.getCode().equals("NOPAT");
        }

        return false;
    }

    public static String getNopatConfidentialityCodeXpathSegment() {
        return """
            confidentialityCode[
                @code='NOPAT' and
                @codeSystem='2.16.840.1.113883.4.642.3.47' and
                @displayName="no disclosure to patient, family or caregivers without attending provider's authorization"
            ]
            """;
    }

    private static Coding getNopatCoding() {
        final String code = "NOPAT";
        final String display = "no disclosure to patient, family or caregivers without attending provider's authorization";

        return getCoding(code, display);
    }

    private static Coding getNoscrubCoding() {
        final String code = "NOSCRUB";
        final String display = "no scrubbing of the patient, family or caregivers without attending provider's authorization";

        return getCoding(code, display);
    }

    private static Coding getCoding(String code, String display) {
        final String system = "http://hl7.org/fhir/v3/ActCode";
        return new Coding().setCode(code)
            .setSystem(system)
            .setDisplay(display);
    }
}