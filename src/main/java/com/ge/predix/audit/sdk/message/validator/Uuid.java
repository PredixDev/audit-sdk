package com.ge.predix.audit.sdk.message.validator;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 *
 * @author Igor Shindel(212579997)
 * @since 25-10-2016
 */

@Documented
@Constraint(validatedBy = UuidValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Uuid {
    String message() default "{Uuid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}