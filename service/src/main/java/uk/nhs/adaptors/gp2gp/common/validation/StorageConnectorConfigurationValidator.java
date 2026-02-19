package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

@Slf4j
public class StorageConnectorConfigurationValidator
        implements ConstraintValidator<ValidStorageConnectorConfiguration, StorageConnectorConfiguration> {

    @Override
    public boolean isValid(StorageConnectorConfiguration config, ConstraintValidatorContext context) {
        TreeMap<String, String> environmentVariables = new TreeMap<>();
        final String MISSING_ENV_VARIABLE_MESSAGE = "Env variable not provided: %s";

        environmentVariables.put("GP2GP_STORAGE_TYPE", config.getType());
        environmentVariables.put("GP2GP_STORAGE_CONTAINER_NAME", config.getContainerName());
        environmentVariables.put("GP2GP_AZURE_STORAGE_CONNECTION_STRING", config.getAzureConnectionString());


        List<String> missingProperties = validateAgainstRuleset(environmentVariables);

        if (!missingProperties.isEmpty()) {
            for (var property : missingProperties) {
                var message = String.format(
                        MISSING_ENV_VARIABLE_MESSAGE,
                        property);

                setConstraintViolation(context, message);
            }
            return false;
        }

        return true;
    }

    private static List<String> validateAgainstRuleset(TreeMap<String, String> environmentVariables) {
        List<String> missingProperties = new ArrayList<>();
        boolean isAzure = environmentVariables.get("GP2GP_STORAGE_TYPE").equals("Azure");

        for (var variable : environmentVariables.entrySet()) {
            boolean isConnectionString = variable.getKey().equals("GP2GP_AZURE_STORAGE_CONNECTION_STRING");

            if (isConnectionString && !isAzure) {
                continue;
            }

            if (StringUtils.isBlank(variable.getValue())) {
                missingProperties.add(variable.getKey());
            }
        }

        return missingProperties;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
