package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.ReconnectMode;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by 212584872 on 1/23/2017.
 */
public class AuditConfigurationTest {

    @Test
    public void testBulkIsTrueByDefault() {
        AuditConfiguration config = AuditConfiguration.builder().build();
        assertThat(config.getBulkMode(),is(true));
    }

    @Test
    public void testBulkIsOff() {
        AuditConfiguration config = AuditConfiguration.builder().build();
        config.setBulkMode(false);
        assertThat(config.getBulkMode(),is(false));
    }

    @Test
    public void testConfigurationDefault(){
        AuditConfiguration config = AuditConfiguration.builder().build();
        assertThat(config.getMaxNumberOfEventsInCache(),is(AuditConfiguration.DEFAULT_CACHE_SIZE));
        assertThat(config.getMaxRetryCount(),is(AuditConfiguration.DEFAULT_RETRY_COUNT));
        assertThat(config.getRetryIntervalMillis(),is(AuditConfiguration.DEFAULT_RETRY_INTERVAL_MILLIS));
        assertThat(config.getReconnectMode(),is(ReconnectMode.MANUAL));

    }

    @Test
    public void testConfigurationDefaultAreOverridden(){
        AuditConfiguration config = AuditConfiguration.builder()
                .maxNumberOfEventsInCache(AuditConfiguration.MIN_CACHE_SIZE)
                .maxRetryCount(7)
                .build();
        assertThat(config.getMaxNumberOfEventsInCache(),is(AuditConfiguration.MIN_CACHE_SIZE));
        assertThat(config.getMaxRetryCount(),is(7));
        assertThat(config.getRetryIntervalMillis(),is(AuditConfiguration.DEFAULT_RETRY_INTERVAL_MILLIS));
      }

    @Test
    public void testConfigurationAuthenticationMode(){
        AuditConfiguration config = AuditConfiguration.builder().build();
        assertThat(config.getAuthenticationMethod(),is(AuthenticationMethod.UAA_USER_PASS));
        assertNull(config.getAuthToken());
    }

    @Test
    public void testConfigurationAuthenticationModeWithAuthToken(){
        AuditConfiguration config = AuditConfiguration.builderWithAuthToken().build();
        assertThat(config.getAuthenticationMethod(),is(AuthenticationMethod.AUTH_TOKEN));
        assertNull(config.getUaaUrl());
        assertNull(config.getUaaClientId());
        assertNull(config.getUaaClientSecret());
    }

    @Test
    public void testConfigurationAuthenticationModeAuthTokenISSet(){
        AuditConfiguration config = AuditConfiguration.builderWithAuthToken()
                .authToken("authToken")
                .build();
        assertThat(config.getAuthenticationMethod(),is(AuthenticationMethod.AUTH_TOKEN));
        assertNull(config.getUaaUrl());
        assertNull(config.getUaaClientId());
        assertNull(config.getUaaClientSecret());
        assertThat(config.getAuthToken(),is("authToken"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAuthTokenConfigurationBuilderWithBadEventhubUrl(){
        AuditConfiguration config = AuditConfiguration.builderWithAuthToken()
                .ehubUrl("1")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigurationWithBadEventhubUrl(){
        AuditConfiguration config = AuditConfiguration.builder()
                .ehubUrl("1")
                .build();
    }

    @Test
    public void testConfigurationWithoutEhubUrl(){
        AuditConfiguration config = AuditConfiguration.builder()
                .ehubUrl(null)
                .build();

        assertNull(config.getEhubHost());
        assertEquals(0, config.getEhubPort());
    }

    @Test
    public void testConfigurationWithEventhubUrl(){
        AuditConfiguration config = AuditConfiguration.builder()
                .ehubUrl("a:123")
                .build();

        assertEquals(123, config.getEhubPort());
        assertEquals("a", config.getEhubHost());
    }
}
