package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = Gp2gpConfigurationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGp2gpConfiguration {
    String message() default "Invalid GP2GP Configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}