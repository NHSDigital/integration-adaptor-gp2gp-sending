package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Slf4j
public class MongoClientConfigurationValidator implements ConstraintValidator<ValidMongoClientConfiguration, MongoClientConfiguration> {

    @Override
    public boolean isValid(MongoClientConfiguration config, ConstraintValidatorContext context) {
        TreeMap<String, String> environmentVariables = new TreeMap<>();
        final String MISSING_ENV_VARIABLE_MESSAGE = "Env variable %s not provided";
//
//        environmentVariables.put("GP2GP_LARGE_ATTACHMENT_THRESHOLD", Integer.toString(config.getLargeAttachmentThreshold()));
//        environmentVariables.put("GP2GP_LARGE_EHR_EXTRACT_THRESHOLD", Integer.toString(config.getLargeEhrExtractThreshold()));
        List<String> missingProperties = new ArrayList<>();
        List<String> invalidProperties = new ArrayList<>();

        for (var variable : environmentVariables.entrySet()) {
            if (StringUtils.isBlank(variable.getValue())) {
                missingProperties.add(variable.getKey());
//                invalidProperties.add(variable.getKey());
            }
        }

//        var presentProperties = environmentVariables.keySet().stream()
//                .filter(key -> !missingProperties.contains(key))
//                .sorted().toList();

        if (!missingProperties.isEmpty()) {
            var message = String.format(
                    MISSING_ENV_VARIABLE_MESSAGE,
                    String.join(", ", missingProperties));

            setConstraintViolation(context, message);
        }

//        if (!invalidProperties.isEmpty()) {
//            var message = String.format(PEM_FORMAT_VIOLATION_MESSAGE, String.join(", ", invalidProperties));
//            setConstraintViolation(context, message);
//        }
//
        if (!missingProperties.isEmpty()) {
            return false;
        }

        return true;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
