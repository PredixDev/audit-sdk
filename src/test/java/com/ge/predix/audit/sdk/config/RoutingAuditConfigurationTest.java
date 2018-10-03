package com.ge.predix.audit.sdk.config;

import com.ge.predix.audit.sdk.exception.RoutingAuditException;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class RoutingAuditConfigurationTest {

    @Test(expected = RoutingAuditException.class)
    public void nullConfigurationThrowsException() throws Exception {
        RoutingAuditConfiguration.builder()
                .appNameConfig(null)
                .sharedAuditConfig(null)
                .systemConfig(null)
                .tenantAuditConfig(null)
                .build();

    }

    @Test(expected = RoutingAuditException.class)
    public void illegalConfigurationThrowsException() throws Exception {
        RoutingAuditConfiguration.builder()
                .appNameConfig(AppNameConfig.builder()
                        .clientSecret("")
                        .clientId("")
                        .uaaUrl("")
                        .appNamePrefix("")
                        .build())
                .sharedAuditConfig(SharedAuditConfig.builder()
                        .auditZoneId("")
                        .ehubUrl("")
                        .ehubZoneId("")
                        .uaaClientId("")
                        .uaaClientSecret("")
                        .tracingToken("")
                        .tracingUrl("")
                        .build())
                .systemConfig(SystemConfig.builder()
                        .build())
                .tenantAuditConfig(TenantAuditConfig.builder().build())
                .build();

    }

    @Test
    public void RoutingConfigurationGreenTest() throws Exception {
        RoutingAuditConfiguration configuration = RoutingAuditConfiguration.builder()
                .appNameConfig(AppNameConfig.builder()
                        .clientSecret("a")
                        .clientId("a")
                        .uaaUrl("a")
                        .build())
                .sharedAuditConfig(SharedAuditConfig.builder()
                        .auditZoneId("b")
                        .ehubUrl("b")
                        .ehubZoneId("b")
                        .uaaClientId("b")
                        .uaaClientSecret("b")
                        .uaaUrl("b")
                        .tracingToken("b")
                        .tracingUrl("b")
                        .build())
                .systemConfig(SystemConfig.builder()
                        .clientSecret("c")
                        .clientId("c")
                        .tokenServiceUrl("url")
                        .tmsUrl("url")
                        .build())
                .tenantAuditConfig(TenantAuditConfig.builder()
                        .auditServiceName("a")
                        .spaceName("c")
                        .build())
                .build();
        assertFalse(configuration.getTenantAuditConfig().isTraceEnabled());
        assertTrue(configuration.getTenantAuditConfig().getBulkMode());
        assertThat(configuration.getTenantAuditConfig().getMaxRetryCount(), is(AbstractAuditConfiguration.DEFAULT_RETRY_COUNT));
        assertThat(configuration.getTenantAuditConfig().getMaxNumberOfEventsInCachePerTenant(), is(AbstractAuditConfiguration.DEFAULT_CACHE_SIZE));
        assertThat(configuration.getTenantAuditConfig().getRetryIntervalMillis(), is(AbstractAuditConfiguration.DEFAULT_RETRY_INTERVAL_MILLIS));
        assertThat(configuration.getAppNameConfig().getAppNamePrefix(), is(AppNameConfig.APP_SCOPE_PREFIX));
        assertThat(configuration.getSystemConfig().getCanonicalServiceName(), is("predix-audit"));
        assertThat(configuration.getRoutingResourceConfig(), is(RoutingResourceConfig.builder()
        .cacheRefreshPeriod(DateTimeConstants.MILLIS_PER_MINUTE * 2)
        .connectionLifetime(DateTimeConstants.MILLIS_PER_MINUTE * 10)
        .maxConcurrentAuditRequest(150000)
        .numOfConnections(100)
        .sharedTenantCacheSize(10000)
        .build()));

    }

    @Test (expected = RoutingAuditException.class)
    public void tracingEnabledWithWrongParamsThrowsException() throws Exception {
        RoutingAuditConfiguration configuration = RoutingAuditConfiguration.builder()
                .appNameConfig(AppNameConfig.builder()
                        .clientSecret("a")
                        .clientId("a")
                        .uaaUrl("a")
                        .build())
                .sharedAuditConfig(SharedAuditConfig.builder()
                        .auditZoneId("b")
                        .ehubUrl("b")
                        .ehubZoneId("b")
                        .uaaClientId("b")
                        .uaaClientSecret("b")
                        .uaaUrl("b")
                        .tracingToken("")
                        .tracingUrl("")
                        .build())
                .systemConfig(SystemConfig.builder()
                        .clientSecret("c")
                        .clientId("c")
                        .tokenServiceUrl("url")
                        .tmsUrl("url")
                        .build())
                .tenantAuditConfig(TenantAuditConfig.builder()
                        .auditServiceName("a")
                        .spaceName("c")
                        .traceEnabled(true)
                        .tracingInterval(0)
                        .build())
                .build();
    }


}