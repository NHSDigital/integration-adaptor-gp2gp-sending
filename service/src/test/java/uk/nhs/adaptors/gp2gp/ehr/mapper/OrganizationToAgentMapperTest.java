package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

@ExtendWith(MockitoExtension.class)
class OrganizationToAgentMapperTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String ORGANIZATION_FILE_LOCATION = "/ehr/mapper/organization/";

    private static final String INPUT_ORGANIZATION_JSON = ORGANIZATION_FILE_LOCATION
        + "input_organization_1.json";
    private static final String OUTPUT_ORGANIZATION_AS_AGENT_PERSON_JSON = ORGANIZATION_FILE_LOCATION
        + "output_organization_1.xml";

    @Test
    void When_MappingOrganization_Expect_AgentResourceXml() {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_ORGANIZATION_JSON);
        var expectedOutput = ResourceTestFileUtils.getFileContent(OUTPUT_ORGANIZATION_AS_AGENT_PERSON_JSON);

        Organization organization = new FhirParseService().parseResource(jsonInput, Organization.class);
        var outputMessage = OrganizationToAgentMapper.mapOrganizationToAgent(organization, TEST_ID);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, INPUT_ORGANIZATION_JSON, OUTPUT_ORGANIZATION_AS_AGENT_PERSON_JSON)
            .isEqualTo(expectedOutput);
    }

    @Test
    void When_MappingOrganizationInnerWithWorkPhone_Expect_TelecomMapped() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setUse(ContactPoint.ContactPointUse.WORK)
                .setValue("0123456789"));

        var outputMessage = OrganizationToAgentMapper.mapOrganizationToAgentInner(organization);

        assertThat(outputMessage).contains("<telecom value=\"tel:0123456789\" use=\"WP\"/>");
    }

    @Test
    void When_MappingOrganizationInnerWithTelecomWithoutSystem_Expect_EhrMapperExceptionThrown() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint().setValue("missing-system-phone"));

        assertThrows(EhrMapperException.class, () -> OrganizationToAgentMapper.mapOrganizationToAgentInner(organization));
    }

    @Test
    void When_MappingOrganizationInnerWithMultipleTelecomsAndFirstMissingSystem_Expect_EhrMapperExceptionThrown() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint().setValue("first-missing-system"));
        organization.addTelecom(new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setUse(ContactPoint.ContactPointUse.WORK)
            .setValue("0123456789"));

        assertThrows(EhrMapperException.class, () -> OrganizationToAgentMapper.mapOrganizationToAgentInner(organization));
    }

    @Test
    void When_TelecomSystemDisplayIsNull_Expect_NoExceptionAndWorkPhoneMappedFromNextEntry() {
        var organization = new Organization();
        var telecomWithNullDisplay = mock(ContactPoint.class);
        var systemWithNullDisplay = mock(ContactPoint.ContactPointSystem.class);

        when(telecomWithNullDisplay.hasSystem()).thenReturn(true);
        when(telecomWithNullDisplay.getSystem()).thenReturn(systemWithNullDisplay);
        when(systemWithNullDisplay.getDisplay()).thenReturn(null);

        organization.getTelecom().add(telecomWithNullDisplay);
        organization.addTelecom(new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setUse(ContactPoint.ContactPointUse.WORK)
            .setValue("07700900000"));

        var outputMessage = assertDoesNotThrow(() -> OrganizationToAgentMapper.mapOrganizationToAgentInner(organization));

        assertThat(outputMessage).contains("<telecom value=\"tel:07700900000\" use=\"WP\"/>");
    }

    @Test
    void When_MappingOrganizationInnerWithNonPhoneSystem_Expect_NoTelecomMapped() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.EMAIL)
            .setUse(ContactPoint.ContactPointUse.WORK)
            .setValue("test@example.com"));

        var outputMessage = OrganizationToAgentMapper.mapOrganizationToAgentInner(organization);

        assertThat(outputMessage).doesNotContain("<telecom");
    }

    @Test
    void When_MappingOrganizationInnerWithPhoneButNoUse_Expect_NoTelecomMapped() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setValue("0123456789"));

        var outputMessage = OrganizationToAgentMapper.mapOrganizationToAgentInner(organization);

        assertThat(outputMessage).doesNotContain("<telecom");
    }

    @Test
    void When_MappingOrganizationInnerWithPhoneButNonWorkUse_Expect_NoTelecomMapped() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setUse(ContactPoint.ContactPointUse.HOME)
            .setValue("0123456789"));

        var outputMessage = OrganizationToAgentMapper.mapOrganizationToAgentInner(organization);

        assertThat(outputMessage).doesNotContain("<telecom");
    }

    @Test
    void When_MappingOrganizationInnerWithTelecomWithoutSystemAndUse_Expect_ExceptionWithCorrectMessage() {
        var organization = new Organization();
        organization.addTelecom(new ContactPoint()
            .setUse(ContactPoint.ContactPointUse.WORK)
            .setValue("test-value"));

        EhrMapperException exception = assertThrows(EhrMapperException.class, 
            () -> OrganizationToAgentMapper.mapOrganizationToAgentInner(organization));
        
        assertEquals("ContactPoint has no system specified", exception.getMessage());
    }
}
