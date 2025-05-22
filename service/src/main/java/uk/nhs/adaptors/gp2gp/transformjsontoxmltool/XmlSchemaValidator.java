package uk.nhs.adaptors.gp2gp.transformjsontoxmltool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;

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

    private static final String VALIDATING_AGAINST_SCHEMA_TEMPLATE = "Validating {} against {} schema";
    private static final String COULD_NOT_LOAD_SCHEMA_FILE_TEMPLATE = "Could not load schema file for {} context.";
    private static final String FAILED_TO_VALIDATE_SCHEMA_TEMPLATE = "Failed to validate {} against {} schema";
    private static final String COULD_NOT_READ_FROM_STREAM_SOURCE_TEMPLATE = "Could not read from stream source for produced XML for {}";
    private static final String SUCCESSFULLY_VALIDATED_SCHEMA_TEMPLATE = "Successfully validated {} against {} schema";
    private static final String VALIDATION_ERRORS_WRITTEN_TEMPLATE = "Validation errors written to {}";
    private static final String COULD_NOT_WRITE_VALIDATION_ERRORS_TEMPLATE = "Could not write validation errors to {}";

    public void validateOutputToXmlSchema(String inputJsonFilename, String xmlResult) {
        LOGGER.info(VALIDATING_AGAINST_SCHEMA_TEMPLATE, inputJsonFilename, RedactionsContext.REDACTION_INTERACTION_ID);

        var xsdErrorHandler = new XsdErrorHandler();
        Validator xmlValidator;

        try {
            xmlValidator = getXmlValidator(xsdErrorHandler);
        } catch (SAXException e) {
            LOGGER.error(COULD_NOT_LOAD_SCHEMA_FILE_TEMPLATE, RedactionsContext.REDACTION_INTERACTION_ID);
            return;
        }

        try {
            var xmlResultSource = new StreamSource(new StringReader(xmlResult));
            xmlValidator.validate(xmlResultSource);
        } catch (SAXParseException parseException) {
            LOGGER.warn(FAILED_TO_VALIDATE_SCHEMA_TEMPLATE, inputJsonFilename, RedactionsContext.REDACTION_INTERACTION_ID);
            writeValidationExceptionsToFile(xsdErrorHandler, inputJsonFilename);
        } catch (IOException e) {
            LOGGER.error(COULD_NOT_READ_FROM_STREAM_SOURCE_TEMPLATE, inputJsonFilename);
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!xsdErrorHandler.isValid()) {
            LOGGER.warn(FAILED_TO_VALIDATE_SCHEMA_TEMPLATE, inputJsonFilename, RedactionsContext.REDACTION_INTERACTION_ID);
            writeValidationExceptionsToFile(xsdErrorHandler, inputJsonFilename);
            return;
        }

        LOGGER.info(SUCCESSFULLY_VALIDATED_SCHEMA_TEMPLATE, inputJsonFilename, RedactionsContext.REDACTION_INTERACTION_ID);
    }

    private void writeValidationExceptionsToFile(XsdErrorHandler xsdErrorHandler, String fileName) {
        String outputFileName = FilenameUtils.removeExtension(fileName) + ".validation-errors.log";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_PATH + outputFileName, StandardCharsets.UTF_8))) {
            for (SAXParseException e : xsdErrorHandler.getExceptions()) {
                var message = String.format("[%d:%d] %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                writer.write(message);
                writer.newLine();
            }
            LOGGER.info(VALIDATION_ERRORS_WRITTEN_TEMPLATE, outputFileName);
        } catch (IOException e) {
            LOGGER.error(COULD_NOT_WRITE_VALIDATION_ERRORS_TEMPLATE, outputFileName, e);
        }
    }

    private Validator getXmlValidator(XsdErrorHandler xsdErrorHandler) throws SAXException {
        var schemaPath = RedactionsContext.REDACTION_INTERACTION_ID.equals(redactionsContext.ehrExtractInteractionId())
            ? RCMR_IN030000UK07_SCHEMA_PATH
            : RCMR_IN030000UK06_SCHEMA_PATH;

        var schemaFactory = SchemaFactory.newDefaultInstance();
        var schemaFileStream = new StreamSource(new File(schemaPath));
        var schema = schemaFactory.newSchema(schemaFileStream);
        var validator = schema.newValidator();

        validator.setErrorHandler(xsdErrorHandler);

        return validator;
    }
}