package uk.nhs.adaptors.gp2gp.common.service;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;

@Service
@Slf4j
public class FhirParseService {

    private final IParser jsonParser = prepareParser();

    public <T extends IBaseResource> T parseResource(String body, Class<T> fhirClass) {
        try {
            return jsonParser.parseResource(fhirClass, body);
        } catch (Exception ex) {
            LOGGER.error("Failed to parse FHIR resource of type {}: {}", fhirClass.getSimpleName(), ex.getMessage(), ex);
            throw new FhirValidationException(ex.getMessage(), ex);
        }
    }

    public String encodeToJson(IBaseResource resource) {
        return jsonParser.setPrettyPrint(true).encodeResourceToString(resource);
    }

    private IParser prepareParser() {
        FhirContext ctx = FhirContext.forDstu3();
        ctx.setParserErrorHandler(new StrictErrorHandler());
        return ctx.newJsonParser();
    }
}
