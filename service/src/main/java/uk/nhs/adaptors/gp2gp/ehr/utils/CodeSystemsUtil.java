package uk.nhs.adaptors.gp2gp.ehr.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeSystemsUtil {

    private static final Map<String, String> SYSTEM_CODES = Map.of(
        "http://snomed.info/sct", "2.16.840.1.113883.2.1.3.2.4.15",
        "https://fhir.hl7.org.uk/Id/egton-codes", "2.16.840.1.113883.2.1.6.3",
        "http://read.info/readv2", "2.16.840.1.113883.2.1.6.2",
        "http://read.info/ctv3", "2.16.840.1.113883.2.1.3.2.4.14"
    );

    public static String getHl7code(String fhirCodeSystem) {
        return SYSTEM_CODES.getOrDefault(fhirCodeSystem, "");
    }
}
