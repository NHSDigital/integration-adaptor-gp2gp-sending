package uk.nhs.adaptors.gp2gp.utils;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.nhs.adaptors.gp2gp.common.utils.OperationOutcomeIssueTypeDeserializer;

class OperationOutcomeIssueTypeDeserializerTest {

    @Test
    void When_DeserializingIssueTypeCodeWithHyphenWhichIsAValidCode_Expect_ThatMemberToBeReturned() {
        var module = new SimpleModule();
        module.addDeserializer(OperationOutcome.IssueType.class, new OperationOutcomeIssueTypeDeserializer());

        var jsonMapper = JsonMapper.builder().build();
        jsonMapper.registerModule(module);

        final var deserializedValue = jsonMapper.convertValue("not-found", OperationOutcome.IssueType.class);

        assertEquals(OperationOutcome.IssueType.NOTFOUND, deserializedValue);
    }

    @Test
    void When_DeserializingIssueTypeCodeWithoutHyphenWhichIsAValidCode_Expect_ThatMemberToBeReturned() {
        var module = new SimpleModule();
        module.addDeserializer(OperationOutcome.IssueType.class, new OperationOutcomeIssueTypeDeserializer());

        var jsonMapper = JsonMapper.builder().build();
        jsonMapper.registerModule(module);

        final var deserializedValue = jsonMapper.convertValue("transient", OperationOutcome.IssueType.class);

        assertEquals(OperationOutcome.IssueType.TRANSIENT, deserializedValue);
    }

    @Test
    void When_DeserializingIssueTypeCodeWhichIsNotAValidCode_Expect_ExceptionThrown() {
        var module = new SimpleModule();
        module.addDeserializer(OperationOutcome.IssueType.class, new OperationOutcomeIssueTypeDeserializer());

        var jsonMapper = JsonMapper.builder().build();
        jsonMapper.registerModule(module);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonMapper.convertValue("not-a-valid-value", OperationOutcome.IssueType.class));
    }
}
