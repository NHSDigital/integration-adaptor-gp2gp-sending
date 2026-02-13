package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.lang.reflect.Type;

@Documented
@Constraint(validatedBy = StorageConnectorConfigurationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStorageConnectorConfiguration {
    String message() default "Invalid File Storage Configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}