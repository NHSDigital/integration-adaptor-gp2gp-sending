package uk.nhs.adaptors.gp2gp.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Paths;

class XPathServiceTest {


    @Test
    @SneakyThrows
    void When_GetMCCIWithValidXPath_Expect_TheMCCIValueIsReturned() {
        final String ACK_TYPE_CODE_XPATH = "//MCCI_IN010000UK13/acknowledgement/@typeCode";
        String basePath = Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath()
                          + "/../../service/src/test/resources/ehr/request/";
        String xmlFilePath = basePath + "MCCI_IN010000UK13_body_AA.xml";

        String validXml = Files.readString(Paths.get(xmlFilePath));

        var document = new XPathService().parseDocumentFromXml(validXml);
        assertEquals("AA", new XPathService().getNodeValue(document, ACK_TYPE_CODE_XPATH));
    }

    @Test
    @SneakyThrows
    void When_ValidXmlIsParsed_Expect_DocumentIsReturned() {
        var document = new XPathService().parseDocumentFromXml("<root/>");
        assertThat(document.getChildNodes().item(0).getNodeName()).isEqualTo("root");
    }

    @Test
    @SneakyThrows
    void When_InvalidXmlIsParsed_Expect_SAXExceptionIsThrown() {
        assertThatExceptionOfType(SAXException.class)
            .isThrownBy(() -> new XPathService().parseDocumentFromXml("NOT XML"));
    }

    @Test
    @SneakyThrows
    void When_GetNodeValueWithValidXPath_Expect_TheValueIsReturned() {
        var document = new XPathService().parseDocumentFromXml("<element>value</element>");
        assertThat(new XPathService().getNodeValue(document, "/element"))
            .isEqualTo("value");
    }

    @Test
    @SneakyThrows
    void When_GetNodeValueWithInvalidXPath_Expect_ThrowsRuntimeException() {
        var document = new XPathService().parseDocumentFromXml("<element>value</element>");
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new XPathService().getNodeValue(document, "!!!NOT XPATH"))
            .withMessageContaining("!!!NOT XPATH");
    }

    @Test
    @SneakyThrows
    void When_GetNodesWithValidXPath_Expect_NodeListReturned() {
        var document = new XPathService().parseDocumentFromXml("<elements><element>value</element><element>value2</element></elements>");
        NodeList nodes = new XPathService().getNodes(document, "//element");
        assertAll(
            () -> assertThat(nodes.getLength()).isEqualTo(2),
            () -> assertThat(nodes.item(0).getTextContent()).isEqualTo("value"),
            () -> assertThat(nodes.item(1).getTextContent()).isEqualTo("value2")
        );
    }
}
