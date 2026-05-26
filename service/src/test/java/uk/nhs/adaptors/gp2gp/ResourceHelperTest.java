package uk.nhs.adaptors.gp2gp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResourceHelperTest {
    @Test
    void When_ValidClasspathResourceProvided_Expect_ResourceLoadedAsString() {
        var actual = ResourceHelper.loadClasspathResourceAsString("/ehr/request/RCMR_IN010000UK05_header.xml");

        assertThat(actual)
            .contains("<soap:Envelope")
            .contains("<eb:ConversationId>DFF5321C-C6EA-468E-BBC2-B0E48000E071</eb:ConversationId>");
    }

    @Test
    void When_ValidXmlClasspathResourceProvided_Expect_ResourceParsedAsXmlDocument() {
        var actual = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");

        assertThat(actual.getDocumentElement().getNodeName()).isEqualTo("soap:Envelope");
    }

    @Test
    void When_ResourcePathIsBlank_Expect_ClearException() {
        assertThatThrownBy(() -> ResourceHelper.loadClasspathResourceAsString(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Classpath resource path must be provided");
    }

    @Test
    void When_ResourceDoesNotExist_Expect_ClearException() {
        assertThatThrownBy(() -> ResourceHelper.loadClasspathResourceAsString("/does/not/exist.xml"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Classpath resource not found: /does/not/exist.xml");
    }
}


