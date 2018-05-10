package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by 212582776 on 2/20/2018.
 */
public class ConfigurationValidatorWithDeprecatedTypeSupportTest {



    private static AuditConfiguration validSyncConfiguration;
    private static AuditConfiguration validAsyncConfiguration;
    private static AuditConfiguration invalidRetryIntervalConfiguration;
    private static AuditConfiguration invalidRetryValueConfiguration;
    private static AuditConfiguration invalidCacheSizeConfiguration;
    private static AuditConfiguration  validSyncConfigurationNoType;

    @Before
    public void init() {

        validSyncConfiguration = AuditConfiguration.builder()
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
                .clientType(AuditClientType.SYNC)
                .build();

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

        validAsyncConfiguration = AuditConfiguration.builder()
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
                .clientType(AuditClientType.ASYNC)
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
    ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(invalidCacheSizeConfiguration, AuditClientType.ASYNC);
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidInterval_throws() throws Exception {
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(invalidRetryIntervalConfiguration, AuditClientType.ASYNC);
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidInvalidRetryCount_throws() throws Exception {
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(invalidRetryValueConfiguration, AuditClientType.ASYNC);
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidSyncAuditType_throws() throws Exception {
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(validSyncConfiguration, AuditClientType.ASYNC);
    }

    @Test(expected = AuditException.class)
    public void validateAuditConfiguration_invalidAsyncAuditType_throws() throws Exception {
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(validAsyncConfiguration, AuditClientType.SYNC);
    }


    @Test
    public void validateAuditConfiguration_noType() throws Exception{
        ConfigurationValidatorFactory.getConfigurationValidator().validateAuditConfiguration(validSyncConfigurationNoType, AuditClientType.SYNC);
    }

}