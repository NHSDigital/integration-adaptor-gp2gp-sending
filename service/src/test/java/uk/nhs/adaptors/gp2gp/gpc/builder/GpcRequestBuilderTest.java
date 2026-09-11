package uk.nhs.adaptors.gp2gp.gpc.builder;

import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.RequestBuilderService;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcClientConfiguration;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;
import java.lang.reflect.Field;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GpcRequestBuilderTest {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final String NHS_NUMBER = "9876543210";
    private static final String OVERRIDE_NHS_NUMBER = "1234567890";
    private static final String CONVERSATION_ID = "conversation-123";
    private static final String FROM_ODS_CODE = "A12345";

    @Mock
    private GpcTokenBuilder gpcTokenBuilder;

    @Mock
    private GpcConfiguration gpcConfiguration;

    @Mock
    private RequestBuilderService requestBuilderService;

    @Mock
    private GpcClientConfiguration gpcClientConfiguration;

    @InjectMocks
    private GpcRequestBuilder gpcRequestBuilder;

    @BeforeEach
    void setUp() throws Exception {
        setOverrideNhsNumber("");
    }

    @Test
    void When_BuildingStructuredRequestBody_WithPatientNhsNumber_Expect_IdentifierSystemAndValueAreSet() {
        GetGpcStructuredTaskDefinition taskDefinition = createStructuredTaskDefinition();

        Parameters parameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(taskDefinition);

        Identifier identifier = (Identifier) parameters.getParameterFirstRep().getValue();

        assertThat(identifier.getSystem()).isEqualTo(NHS_NUMBER_SYSTEM);
        assertThat(identifier.getValue()).isEqualTo(NHS_NUMBER);
    }

    @Test
    void When_BuildingStructuredRequestBody_WithOverrideNhsNumber_Expect_OverrideValueIsUsed() throws Exception {
        setOverrideNhsNumber(OVERRIDE_NHS_NUMBER);
        GetGpcStructuredTaskDefinition taskDefinition = createStructuredTaskDefinition();

        Parameters parameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(taskDefinition);

        Identifier identifier = (Identifier) parameters.getParameterFirstRep().getValue();

        assertThat(identifier.getSystem()).isEqualTo(NHS_NUMBER_SYSTEM);
        assertThat(identifier.getValue()).isEqualTo(OVERRIDE_NHS_NUMBER);
    }

    @Test
    void When_BuildingStructuredRequestBody_WithWhitespaceOverrideNhsNumber_Expect_PatientNhsNumberIsUsed() throws Exception {
        setOverrideNhsNumber("   ");
        GetGpcStructuredTaskDefinition taskDefinition = createStructuredTaskDefinition();

        Parameters parameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(taskDefinition);

        Identifier identifier = (Identifier) parameters.getParameterFirstRep().getValue();

        assertThat(identifier.getSystem()).isEqualTo(NHS_NUMBER_SYSTEM);
        assertThat(identifier.getValue()).isEqualTo(NHS_NUMBER);
    }

    private GetGpcStructuredTaskDefinition createStructuredTaskDefinition() {
        return GetGpcStructuredTaskDefinition.builder()
            .conversationId(CONVERSATION_ID)
            .fromOdsCode(FROM_ODS_CODE)
            .nhsNumber(NHS_NUMBER)
            .build();
    }

    private void setOverrideNhsNumber(String value) throws Exception {
        Field field = GpcRequestBuilder.class.getDeclaredField("overrideNhsNumber");
        field.setAccessible(true);
        field.set(gpcRequestBuilder, value);
    }
}
