package com.ge.predix.audit.sdk.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by 212582776 on 3/17/2018.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { EnvUtils.class })
public class EnvUtilsTest {

    public static final String MY_ENV = "my_env";
    public static final String MY_VALUE = "my_value";
    public static final String BAD_VAR = "BAD_VAR";

    @Before
    public void init(){
        EnvUtils.clear();
    }


    @Test
    public void testEnvIsNull(){
        assertNull(EnvUtils.getEnvironmentVar(BAD_VAR));
        assertThat(EnvUtils.variables.size(),is(1));
        assertTrue(EnvUtils.variables.containsKey(BAD_VAR));
    }

    @Test
    public void testMockEnv_valueIsReturned(){
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(MY_ENV)).thenReturn(MY_VALUE);
        assertThat(EnvUtils.getEnvironmentVar(MY_ENV), is(MY_VALUE));
    }

}
