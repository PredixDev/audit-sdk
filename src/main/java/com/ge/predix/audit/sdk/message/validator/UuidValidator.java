package com.ge.predix.audit.sdk.message.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.UUID;

/**
 *
 * @author Igor Shindel(212579997)
 * @since 25-10-2016
 */

public class UuidValidator implements ConstraintValidator<Uuid, String> {

    @Override
    public void initialize(Uuid uuid) {
    }

    public boolean isValid(String uuid, ConstraintValidatorContext ctx) {
        boolean result = true;
        if(null != uuid && !uuid.isEmpty()) {
            try {
                UUID.fromString(uuid);
            }catch(Exception e){
                result = false;
            }
        }
        return result;
    }
}