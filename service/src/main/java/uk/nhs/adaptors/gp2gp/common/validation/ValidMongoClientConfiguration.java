package uk.nhs.adaptors.gp2gp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = MongoClientConfigurationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMongoClientConfiguration {
    String message() default "Invalid Mongo Client Configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}