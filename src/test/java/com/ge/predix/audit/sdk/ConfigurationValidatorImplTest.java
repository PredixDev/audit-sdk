package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by 212582776 on 2/20/2018.
 */
public class ConfigurationValidatorImplTest {

    private static AuditConfiguration invalidRetryIntervalConfiguration;
    private static AuditConfiguration invalidRetryValueConfiguration;
    private static AuditConfiguration invalidCacheSizeConfiguration;
    private static AuditConfiguration  validSyncConfigurationNoType;

    @Before
    public void init() {


        validSyncConfigurationNoType = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .uaaUrl("http://localhost:443/uaa")
                .tracingInterval(300)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .bulkMode(false)
                .build();


        invalidRetryIntervalConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .uaaUrl("http://localhost:443/uaa")
                .tracingInterval(300)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .bulkMode(false)
                .retryIntervalMillis(1000)
                .build();


        invalidRetryIntervalConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .uaaUrl("http://localhost:443/uaa")
                .tracingInterval(300)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .bulkMode(false)
                .retryIntervalMillis(1000)
                .build();


        invalidRetryValueConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .tracingInterval(300)
                .tracingToken("token")
                .tracingUrl("http://localhost:443/tracing")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(false)
                .maxRetryCount(50)
                .build();

        invalidCacheSizeConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .tracingInterval(300)
                .tracingToken("token")
                .tracingUrl("http://localhost:443/tracing")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(false)
                .maxNumberOfEventsInCache(3)
                .build();
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidCacheSize_throws() throws Exception {
    ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(invalidCacheSizeConfiguration);
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidInterval_throws() throws Exception {
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(invalidRetryIntervalConfiguration);
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidInvalidRetryCount_throws() throws Exception {
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(invalidRetryValueConfiguration);
    }

    @Test
    public void validateAuditConfiguration_noType() throws Exception{
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(validSyncConfigurationNoType);
    }

}