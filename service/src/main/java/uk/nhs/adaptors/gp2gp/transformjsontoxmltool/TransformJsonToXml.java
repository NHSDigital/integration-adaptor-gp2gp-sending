package uk.nhs.adaptors.gp2gp.transformjsontoxmltool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrValidationException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SpringBootApplication
@ComponentScan("uk.nhs.adaptors.gp2gp")
@ConditionalOnProperty(
    prefix = "command.line.runner",
    value = "enabled",
    havingValue = "true")
@Component
public class TransformJsonToXml implements CommandLineRunner {
    private static final String JSON_FILE_INPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/input/";
    private static final String XML_OUTPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/output/";
    private final FhirParseService fhirParseService;
    private final MessageContext messageContext;
    private final OutputMessageWrapperMapper outputMessageWrapperMapper;
    private final EhrExtractMapper ehrExtractMapper;

    public static void main(String[] args) {
        SpringApplication.run(TransformJsonToXml.class, args).close();
    }

    @Override
    public void run(String... args) {
        try {
            getFiles().forEach(file -> {
                String xmlResult = mapJsonToXml(file.getJsonFileInput());
                writeToFile(xmlResult, file.getJsonFileName());
            });
        } catch (NHSNumberNotFound | UnreadableJsonFileException | NoJsonFileFound | Hl7TranslatedResponseError e) {
            LOGGER.error("Failed to parse the provided JSON: {}", e.getMessage());
        } catch (EhrValidationException e) {
            LOGGER.error("Failed to validate the produced XML");
        }
    }

    private List<InputFile> getFiles() throws UnreadableJsonFileException, NoJsonFileFound {
        File[] files = new File(JSON_FILE_INPUT_PATH).listFiles();

        if (files == null || files.length == 0) {
            throw new NoJsonFileFound("No json files found");
        }

        LOGGER.info("Processing {} files from location: {}", files.length, JSON_FILE_INPUT_PATH);

        return Arrays.stream(files)
            .filter(file -> FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("json"))
            .map(file -> {
                try {
                    String jsonAsString = readJsonFileAsString(JSON_FILE_INPUT_PATH + file.getName());
                    return new InputFile(file.getName(), jsonAsString);
                } catch (IOException e) {
                    throw new UnreadableJsonFileException("Cannot read Json File as String: " + file.getName());
                }
            }).collect(Collectors.toList());

    }

    final String mapJsonToXml(String jsonAsStringInput) {
        String hl7TranslatedResponse;
        try {
            final Bundle bundle = new FhirParseService().parseResource(jsonAsStringInput, Bundle.class);

            messageContext.initialize(bundle);

            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

            String gp2gptest = "GP2GPTEST";
            getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
                .nhsNumber(extractNhsNumber(jsonAsStringInput))
                .conversationId("6910A49D-1F97-4AA0-9C69-197EE9464C76")
                .requestId("17A3A644-A4EB-4C0A-A870-152D310FD1F8")
                .fromOdsCode(gp2gptest)
                .toOdsCode(gp2gptest)
                .toAsid(gp2gptest)
                .fromAsid(gp2gptest)
                .build();

            final EhrExtractTemplateParameters ehrExtractTemplateParameters =
                ehrExtractMapper.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition, bundle);

            final String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

            hl7TranslatedResponse = outputMessageWrapperMapper.map(getGpcStructuredTaskDefinition, ehrExtractContent);
        } catch (Hl7TranslatedResponseError e) {
            throw new Hl7TranslatedResponseError("Could not get hl7TranslatedResponse");
        } finally {
            messageContext.resetMessageContext();
        }
        return hl7TranslatedResponse;
    }

    private void writeToFile(String xml, String sourceFileName) {
        String outputFileName = FilenameUtils.removeExtension(sourceFileName);
        try (BufferedWriter writer =
                 new BufferedWriter(new FileWriter(XML_OUTPUT_PATH + outputFileName + ".xml", StandardCharsets.UTF_8))) {
            writer.write(xml);
            LOGGER.info("Contents of file: {}. Saved to: {}.xml", sourceFileName, outputFileName);
        } catch (IOException e) {
            LOGGER.error("Could not send Xml result to the file", e);
        }
    }

    private String readJsonFileAsString(String file) throws IOException {
        return Files.readString(Paths.get(file));
    }

    private String extractNhsNumber(String json) throws NHSNumberNotFound {
        var nhsNumberSystem = "https://fhir.nhs.uk/Id/nhs-number";
        var bundle = fhirParseService.parseResource(json, Bundle.class);
        return bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> ResourceType.Patient.equals(resource.getResourceType()))
            .map(Patient.class::cast)
            .map(resource -> getNhsNumberIdentifier(nhsNumberSystem, resource))
            .findFirst()
            .orElseThrow(() -> new NHSNumberNotFound("No Patient identifier was found"))
            .getValue();
    }

    private Identifier getNhsNumberIdentifier(String nhsNumberSystem, Patient resource) {
        return resource.getIdentifier().stream()
            .filter(identifier -> identifier.getSystem().equals(nhsNumberSystem))
            .findFirst()
            .orElseThrow(() -> new NHSNumberNotFound("No Patient identifier was found"));
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class InputFile {
        private String jsonFileName;
        private String jsonFileInput;
    }

    public static class UnreadableJsonFileException extends RuntimeException {
        public UnreadableJsonFileException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class NoJsonFileFound extends RuntimeException {
        public NoJsonFileFound(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class NHSNumberNotFound extends RuntimeException {
        public NHSNumberNotFound(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class Hl7TranslatedResponseError extends RuntimeException {
        public Hl7TranslatedResponseError(String errorMessage) {
            super(errorMessage);
        }
    }
}