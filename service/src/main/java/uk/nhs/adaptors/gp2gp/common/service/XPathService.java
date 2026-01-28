package uk.nhs.adaptors.gp2gp.common.service;

import static javax.xml.xpath.XPathConstants.NODESET;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import lombok.SneakyThrows;

@Component
public class XPathService {

    private static final Map<String, XPathExpression> CACHE = new ConcurrentHashMap<>();
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newDefaultInstance();

    private XPathExpression compile(String expression) {
        return CACHE.computeIfAbsent(expression, expr -> {
            try {
                return XPathFactory.newInstance()
                    .newXPath()
                    .compile(expr);
            } catch (XPathExpressionException e) {
                throw new IllegalArgumentException("Invalid xpath: " + expr, e);
            }
        });
    }

    public Document parseDocumentFromXml(String xml) throws SAXException {

        try {
            DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xml));
            return documentBuilder.parse(inputSource);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to configure XML parser", e);
        } catch (IOException e) {
            throw new RuntimeException("IO error while reading XML", e);
        }
    }

    public String getNodeValue(Document xmlDoc, String expression) {
        try {
            return (String) compile(expression).evaluate(xmlDoc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid xpath expression " + expression, e);
        }
    }

    public String getNodeValue(Document xmlDoc, String firstExpression, String secondExpression) {
        var firstExtraction = getNodeValue(xmlDoc, firstExpression);
        return StringUtils.isNotBlank(firstExtraction)
               ? firstExtraction
               : getNodeValue(xmlDoc, secondExpression);
    }

    @SneakyThrows
    public NodeList getNodes(Document document, String xPath) {
        try {
            return (NodeList) compile(xPath).evaluate(document, NODESET);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid xpath expression " + xPath, e);
        }
    }
}
