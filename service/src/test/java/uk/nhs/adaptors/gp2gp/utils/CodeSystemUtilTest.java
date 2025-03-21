package uk.nhs.adaptors.gp2gp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeSystemsUtil;

import java.util.stream.Stream;

public class CodeSystemUtilTest {

    private static Stream<Arguments> knownCodeSystems() {
        return Stream.of(
            Arguments.of("http://snomed.info/sct", "2.16.840.1.113883.2.1.3.2.4.15"),
            Arguments.of("https://fhir.hl7.org.uk/Id/egton-codes", "2.16.840.1.113883.2.1.6.3"),
            Arguments.of("http://read.info/readv2", "2.16.840.1.113883.2.1.6.2"),
            Arguments.of("http://read.info/ctv3", "2.16.840.1.113883.2.1.3.2.4.14"),
            Arguments.of("https://fhir.hl7.org.uk/Id/emis-drug-codes", "2.16.840.1.113883.2.1.6.9")
        );
    }

    @ParameterizedTest
    @MethodSource("knownCodeSystems")
    void When_FhirCodeSystemIsKnown_Expect_CorrectHl7Code(String fhirCodeSystem, String expectedHl7Code) {
        var hl7Code = CodeSystemsUtil.getHl7code(fhirCodeSystem);

        assertThat(hl7Code).isEqualTo(expectedHl7Code);
    }

    @Test
    void When_FhirCodeSystemIsUnknown_Expect_EmptyString() {
        var hl7Code = CodeSystemsUtil.getHl7code("https://unknown.code/system");

        assertThat(hl7Code).isEmpty();
    }
}
