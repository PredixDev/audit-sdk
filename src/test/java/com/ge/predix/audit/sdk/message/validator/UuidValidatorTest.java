package com.ge.predix.audit.sdk.message.validator;

import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by 212584872 on 1/18/2017.
 */
public class UuidValidatorTest {


    @Test
    public void initializeGreenTestTest() throws Exception {
        UuidValidator validator = new UuidValidator();
        Uuid uuid = Mockito.mock(Uuid.class);
        validator.initialize(uuid);
    }

    @Test
    public void isValidTest() throws Exception {
        UuidValidator validator = new UuidValidator();
        assertThat(validator.isValid(UUID.randomUUID().toString(),
                Mockito.mock(ConstraintValidatorContext.class)
        ),is(true));
    }

    @Test
    public void isValidFailTest() throws Exception {
        UuidValidator validator = new UuidValidator();
        String invalidUuid ="Im trying to trick you!";
        assertThat(validator.isValid(invalidUuid,
                Mockito.mock(ConstraintValidatorContext.class)
                ),is(false));
    }

}