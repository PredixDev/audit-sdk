package com.ge.predix.audit.sdk.message.validator;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Igor on 02/02/2017.
 */
public class UuidValidator1Test {

    private UuidValidator validator;

    private static String UUID_VALID = UUID.nameUUIDFromBytes("VALID".getBytes(Charset.defaultCharset())).toString();
    private static String UUID_INVALID = "234";

    @Before
    public void setUp() throws Exception {
        validator = new UuidValidator();
    }

    @Test
    public void isValidGreenTest() throws Exception {
        assertThat(validator.isValid(UUID_VALID, null), is(true));
        assertThat(validator.isValid(null, null), is(true));
    }

    @Test
    public void isValidFailTest() throws Exception {
        assertThat(validator.isValid(UUID_INVALID, null), is(false));
    }
}