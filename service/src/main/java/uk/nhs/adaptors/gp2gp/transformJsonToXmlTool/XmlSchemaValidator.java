package uk.nhs.adaptors.gp2gp.transformjsontoxmltool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Component
@Slf4j
@RequiredArgsConstructor()
public class XmlSchemaValidator {
    private final RedactionsContext redactionsContext;

    private static final String SCHEMA_PATH = "../service/src/test/resources/mim/Schemas/";
    private static final String RCMR_IN030000UK06_SCHEMA_PATH =  SCHEMA_PATH + RedactionsContext.NON_REDACTION_INTERACTION_ID + ".xsd";
    private static final String RCMR_IN030000UK07_SCHEMA_PATH =  SCHEMA_PATH + RedactionsContext.REDACTION_INTERACTION_ID + ".xsd";
    private static final String OUTPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/output/";

    public void validateOutputToXmlSchema(String filename, String xmlResult) {
        LOGGER.info("Validating {} against {} schema", filename, RedactionsContext.REDACTION_INTERACTION_ID);

        var xsdErrorHandler = new uk.nhs.adaptors.gp2gp.transformjsontoxmltool.XsdErrorHandler();
        Validator xmlValidator;

        try {
            xmlValidator = getXmlValidator(xsdErrorHandler);
        } catch (SAXException e) {
            LOGGER.info("Could not load schema file for {} context.", RedactionsContext.REDACTION_INTERACTION_ID);
            return;
        }

        try {
            var xmlResultSource = new StreamSource(new StringReader(xmlResult));
            xmlValidator.validate(xmlResultSource);
        } catch (SAXParseException parseException) {
            LOGGER.info("Failed to validate {} against {} schema", filename, RedactionsContext.REDACTION_INTERACTION_ID);
            writeValidationExceptionsToFile(xsdErrorHandler, filename);
        } catch (IOException e) {
            LOGGER.info("Could not read from stream source for produced XML for {}", filename);
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!xsdErrorHandler.isValid()) {
            LOGGER.info("Failed to validate {} against {} schema", filename, RedactionsContext.REDACTION_INTERACTION_ID);
            writeValidationExceptionsToFile(xsdErrorHandler, filename);
            return;
        }

        LOGGER.info("Successfully validated {} against {} schema", filename, RedactionsContext.REDACTION_INTERACTION_ID);
    }

    private void writeValidationExceptionsToFile(
        uk.nhs.adaptors.gp2gp.transformjsontoxmltool.XsdErrorHandler xsdErrorHandler,
        String fileName
    ) {
        String outputFileName = FilenameUtils.removeExtension(fileName) + ".validation-errors.log";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_PATH + outputFileName, StandardCharsets.UTF_8))) {
            for (SAXParseException e : xsdErrorHandler.getExceptions()) {
                var message = String.format("[%d:%d] %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                writer.write(message);
                writer.newLine();
            }
            LOGGER.info("Validation errors written to {}", outputFileName);
        } catch (IOException e) {
            LOGGER.error("Could not write validation errors to {}", outputFileName, e);
        }
    }

    private Validator getXmlValidator(uk.nhs.adaptors.gp2gp.transformjsontoxmltool.XsdErrorHandler xsdErrorHandler) throws SAXException {
        var schemaPath = RedactionsContext.REDACTION_INTERACTION_ID.equals(redactionsContext.ehrExtractInteractionId())
            ? RCMR_IN030000UK07_SCHEMA_PATH
            : RCMR_IN030000UK06_SCHEMA_PATH;

        var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        var schemaFileStream = new StreamSource(new File(schemaPath));
        var schema = schemaFactory.newSchema(schemaFileStream);
        var validator = schema.newValidator();

        validator.setErrorHandler(xsdErrorHandler);

        return validator;
    }
}
