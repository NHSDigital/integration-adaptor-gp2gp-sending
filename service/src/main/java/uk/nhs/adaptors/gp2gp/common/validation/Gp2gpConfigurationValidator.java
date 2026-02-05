package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Slf4j
public class Gp2gpConfigurationValidator implements ConstraintValidator<ValidGp2gpConfiguration, Gp2gpConfiguration> {

    @Override
    public boolean isValid(Gp2gpConfiguration config, ConstraintValidatorContext context) {
        TreeMap<String, Integer> environmentVariables = new TreeMap<>();
        environmentVariables.put("GP2GP_LARGE_ATTACHMENT_THRESHOLD", config.getLargeAttachmentThreshold());
        environmentVariables.put("GP2GP_LARGE_EHR_EXTRACT_THRESHOLD", config.getLargeEhrExtractThreshold());
        List<String> missingProperties = new ArrayList<>();
        List<String> invalidProperties = new ArrayList<>();

        for (var variable : environmentVariables.entrySet()) {
            if (StringUtils.isBlank(variable.getValue())) {
                missingProperties.add(variable.getKey());
                invalidProperties.add(variable.getKey());
            }
        }

        var presentProperties = environmentVariables.keySet().stream()
                .filter(key -> !missingProperties.contains(key))
                .sorted().toList();

        if (!missingProperties.isEmpty()) {
            var message = String.format(
                    SSL_PROPERTIES_VIOLATION_MESSAGE,
                    String.join(", ", missingProperties),
                    String.join(", ", presentProperties));

            setConstraintViolation(context, message);
        }

        if (!invalidProperties.isEmpty()) {
            var message = String.format(PEM_FORMAT_VIOLATION_MESSAGE, String.join(", ", invalidProperties));
            setConstraintViolation(context, message);
        }

        if (!missingProperties.isEmpty() || !invalidProperties.isEmpty()) {
            config.setSslEnabled(false);
            return false;
        }

        config.setSslEnabled(true);
        return true;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
