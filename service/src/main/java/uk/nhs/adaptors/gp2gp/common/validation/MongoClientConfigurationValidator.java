package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;

import java.util.ArrayList;
import java.util.TreeMap;

@Slf4j
public class MongoClientConfigurationValidator implements ConstraintValidator<ValidMongoClientConfiguration, MongoClientConfiguration> {
    private MongoClientConfiguration configuration;
    private static final String MISSING_ENV_VARIABLE_MESSAGE =
            "Env variable not provided: %s. Alternatively, provide the following env variables instead: %s";

    @Override
    public boolean isValid(MongoClientConfiguration config, ConstraintValidatorContext context) {
        TreeMap<String, String> environmentVariables = getEnvironmentVariables(config);

        ArrayList<String> validationMessages = validateAgainstRuleset(environmentVariables);

        if (!validationMessages.isEmpty()) {
            for (var message : validationMessages) {
                setConstraintViolation(context, message);
            }
            return false;
        }
        return true;
    }

    private TreeMap<String, String> getEnvironmentVariables(MongoClientConfiguration configuration) {
        TreeMap<String, String> environmentVariables =  new TreeMap<>();

        environmentVariables.put("GP2GP_MONGO_URI", configuration.getUri());
        environmentVariables.put("GP2GP_MONGO_DATABASE", configuration.getDatabase());
        environmentVariables.put("GP2GP_MONGO_HOST", configuration.getHost());
        environmentVariables.put("GP2GP_MONGO_PORT", configuration.getPort());
        environmentVariables.put("GP2GP_MONGO_USERNAME", configuration.getUsername());
        environmentVariables.put("GP2GP_MONGO_PASSWORD", configuration.getPassword());
        environmentVariables.put("GP2GP_MONGO_OPTIONS", configuration.getOptions());

        return environmentVariables;
    }

    private ArrayList<String> validateAgainstRuleset(TreeMap<String, String> environmentVariables) {
        ArrayList<String> messages = new ArrayList<>();
        boolean uriProvided = !environmentVariables.get("GP2GP_MONGO_URI").isBlank();

        if (!uriProvided) {
            ArrayList<String> validatedProperties = new ArrayList<>();
            for (var variable : environmentVariables.entrySet()) {
                if (StringUtils.isBlank(variable.getValue()) && !variable.getKey().equals("GP2GP_MONGO_URI")) {
                    validatedProperties.add(variable.getKey());
                }
            }

            if (!validatedProperties.isEmpty()) {
                messages.add(String.format(this.MISSING_ENV_VARIABLE_MESSAGE, "GP2GP_MONGO_URI", String.join(", ", validatedProperties)));
            }
        }

        return messages;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
