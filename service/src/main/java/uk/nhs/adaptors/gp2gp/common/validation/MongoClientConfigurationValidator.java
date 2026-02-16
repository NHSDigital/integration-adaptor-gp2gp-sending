package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;

import java.util.TreeMap;

@Slf4j
public class MongoClientConfigurationValidator implements ConstraintValidator<ValidMongoClientConfiguration, MongoClientConfiguration> {
    private MongoClientConfiguration configuration;


    @Override
    public boolean isValid(MongoClientConfiguration config, ConstraintValidatorContext context) {
        this.configuration = config;
        final String MISSING_ENV_VARIABLE_MESSAGE = "Env variable %s not provided";

        TreeMap<String, String> environmentVariables = getEnvironmentVariables();

        ValidatedProperties properties = validateAgainstRuleset(environmentVariables);

        if (!properties.missingProperties.isEmpty()) {
            for(var property : properties.missingProperties)
            {
                var message = String.format(
                        MISSING_ENV_VARIABLE_MESSAGE,
                        property);

                setConstraintViolation(context, message);
            }
            return false;
        }

        return true;
    }

    private TreeMap<String, String> getEnvironmentVariables()
    {
        TreeMap<String, String> environmentVariables =  new TreeMap<>();

        environmentVariables.put("GP2GP_MONGO_URI", configuration.getUri());
        environmentVariables.put("GP2GP_MONGO_DATABASE", configuration.getDatabase());
        environmentVariables.put("GP2GP_MONGO_HOST", configuration.getHost());
        environmentVariables.put("GP2GP_MONGO_PORT", configuration.getPort());
        environmentVariables.put("GP2GP_MONGO_USERNAME", configuration.getUsername());
        environmentVariables.put("GP2GP_MONGO_PASSWORD", configuration.getPassword());

        return environmentVariables;
    }

    // TODO: add custom rules
    private static ValidatedProperties validateAgainstRuleset(TreeMap<String, String> environmentVariables)
    {
        ValidatedProperties validatedProperties = new ValidatedProperties();
        boolean uriProvided = !environmentVariables.get("GP2GP_MONGO_URI").isBlank();

        for (var variable : environmentVariables.entrySet()) {
            if(uriProvided) {
                if (variable.getKey().equals("GP2GP_MONGO_URI")) continue;
            }
            else
            {
                if (StringUtils.isBlank(variable.getValue())) {
                    validatedProperties.missingProperties.add(variable.getKey());
                }
            }
        }

        // If all other env variables are present then ignore absence of uri property
        if(!uriProvided && validatedProperties.missingProperties.size() == 1) {
            validatedProperties.missingProperties.clear();
        }

        return validatedProperties;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
