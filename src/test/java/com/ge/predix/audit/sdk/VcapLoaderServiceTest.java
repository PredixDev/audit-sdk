package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.*;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.config.vcap.VcapServices;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import com.ge.predix.audit.sdk.util.EnvUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by Igor on 20/11/2016.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { EnvUtils.class })
public class VcapLoaderServiceTest {

    public static final String AUDIT_SERVICE = "audit-service";
    public static final String UAA_URL = "http://localhost:8090";
    public static final String UAA_CLIENT_ID = "admin";
    public static final String UAA_CLIENT_SECRET = "admin";

    private VcapLoaderServiceImpl vcapLoaderService;

    @Before
    public void setUp() throws InterruptedException {
        vcapLoaderService = new VcapLoaderServiceImpl();
        vcapLoaderService.setAuditServiceName(AUDIT_SERVICE);
        vcapLoaderService.setUaaUrl(UAA_URL);
        vcapLoaderService.setUaaClientId(UAA_CLIENT_ID);
        vcapLoaderService.setUaaClientSecret(UAA_CLIENT_SECRET);
    }

    @Test
    public void loadVcapServicesGreenTest() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        vcapLoaderService.getConfigFromVcap();
        assertThat(vcapLoaderService.getVcapServices().getAuditService().size(), is(1));
    }

    @Test
    public void loadVcapServicesWithParametersGreenTest() throws IOException, VcapLoadException {
        String vcapServicesText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_SERVICES_CORRECT.json")));
        String vcapApplicationText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_APPLICATION.json")));
        VcapLoaderServiceImpl vcapLoaderServiceParam = new VcapLoaderServiceImpl(AUDIT_SERVICE, vcapServicesText);
        vcapLoaderServiceParam.setUaaUrl(UAA_URL);
        vcapLoaderServiceParam.setUaaClientId(UAA_CLIENT_ID);
        vcapLoaderServiceParam.setUaaClientSecret(UAA_CLIENT_SECRET);
        vcapLoaderServiceParam.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderServiceParam.getConfigFromVcap();
        assertThat(vcapLoaderServiceParam.getVcapServices().getAuditService().size(), is(1));
    }

    @Test
    public void loadVcapServicesTracingGreenTest() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        vcapLoaderService.getConfigFromVcap();
        assertThat(vcapLoaderService.getVcapServices().getAuditService().size(), is(1));
        assertThat(vcapLoaderService.getVcapServices().getAuditService().get(0).getCredentials()
                .getTracingInterval(), is(900000L));
        assertThat(vcapLoaderService.getVcapServices().getAuditService().get(0).getCredentials()
                .getTracingUrl(), is("https://message-tracing.run.asv-pr.ice.predix.io/v1/checpoint"));
    }

    @Test(expected = VcapLoadException.class)
    public void loadVcapServicesWRONGTest() throws IOException, VcapLoadException {
        String vcapServicesText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_SERVICES_WRONG.json")));
        String vcapApplicationText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_APPLICATION.json")));
        vcapLoaderService.setVcapServicesEnv(vcapServicesText);
        vcapLoaderService.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderService.getConfigFromVcap();
    }

    @Test(expected = VcapLoadException.class)
    public void getApplicationFromVcapWRONGTest() throws IOException, VcapLoadException {
        String vcapApplicationText = "{[]";
        vcapLoaderService.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderService.getApplicationFromVcap();
    }

    @Test
    public void loadVcapApplicationTest() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        vcapLoaderService.getConfigFromVcap();
        assertThat(vcapLoaderService.getVcapApplication().getAppName() != null &&
                !vcapLoaderService.getVcapApplication().getAppName().isEmpty(), is(true));
    }

    @Test(expected = VcapLoadException.class)
    public void loadVcapApplicationMultipleAuditTest() throws IOException, VcapLoadException {
        String vcapServicesText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_SERVICES_MULTIPLE_AUDIT.json")));
        String vcapApplicationText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_APPLICATION.json")));
        vcapLoaderService.setVcapServicesEnv(vcapServicesText);
        vcapLoaderService.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderService.getConfigFromVcap();
    }

    @Test(expected = VcapLoadException.class)
    public void loadVcapApplicationWrongValuesTest() throws IOException, VcapLoadException {
        String vcapServicesText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_SERVICES_WRONG_VALUES.json")));
        String vcapApplicationText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_APPLICATION.json")));
        vcapLoaderService.setVcapServicesEnv(vcapServicesText);
        vcapLoaderService.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderService.getConfigFromVcap();
        assertThat(vcapLoaderService.getVcapApplication().getAppName() != null &&
                !vcapLoaderService.getVcapApplication().getAppName().isEmpty(), is(true));
    }

    @Test(expected = VcapLoadException.class)
    public void loadVcapApplicationNoAuditTest() throws IOException, VcapLoadException {
        String vcapServicesText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_SERVICES_NO_AUDIT.json")));
        String vcapApplicationText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_APPLICATION.json")));
        vcapLoaderService.setVcapServicesEnv(vcapServicesText);
        vcapLoaderService.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderService.getConfigFromVcap();
        assertThat(vcapLoaderService.getVcapApplication().getAppName() != null &&
                !vcapLoaderService.getVcapApplication().getAppName().isEmpty(), is(true));
    }


    private void setValidVcapServicesAndApplication() throws IOException {
        String vcapServicesText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_SERVICES_CORRECT.json")));
        String vcapApplicationText = new String(Files.readAllBytes(
                Paths.get("src/test/resources/VCAP_APPLICATION.json")));
        vcapLoaderService.setVcapServicesEnv(vcapServicesText);
        vcapLoaderService.setVcapApplicationEnv(vcapApplicationText);
    }

    @Test
    public void vcapServicesEqualsTest() throws IOException, VcapLoadException {
        VcapServices vcapServices1 = getVcapServices(
                "src/test/resources/VCAP_SERVICES_CORRECT.json",
                "src/test/resources/VCAP_APPLICATION.json");
        VcapServices vcapServices2 = getVcapServices(
                "src/test/resources/VCAP_SERVICES_CORRECT1.json",
                "src/test/resources/VCAP_APPLICATION.json");

        assertThat(vcapServices1.equals(vcapServices2), is(false));
    }

    @Test
    public void loadConfigurationFromVcapOptionalParamsNotPresent_ConfigurationIsCreatedWithDefault() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
        assertNull(configFromVcap.getAuthToken());
        assertThat(configFromVcap.getRetryIntervalMillis(),is(AuditConfiguration.DEFAULT_RETRY_INTERVAL_MILLIS));
        assertThat(configFromVcap.getMaxRetryCount(),is(AuditConfiguration.DEFAULT_RETRY_COUNT));
        assertThat(configFromVcap.getReconnectMode(),is(ReconnectMode.MANUAL));
        assertNull(configFromVcap.getClientType());
    }

    @Test
    public void loadRoutingConfigurationFromVcapOptionalParamsNotPresent_ConfigurationIsCreatedWithDefault() throws IOException, VcapLoadException {
        RoutingAuditConfiguration expected = setUpDefaultRoutingConfig();

        RoutingAuditConfiguration configuration = vcapLoaderService.getRoutingConfigFromVcap();

        assertEquals(expected, configuration);
    }

    private RoutingAuditConfiguration setUpDefaultRoutingConfig() throws IOException {
        setValidVcapServicesAndApplication();

        String auditServiceName = AUDIT_SERVICE;
        String auditUaaUrl = UAA_URL;
        String auditUaaClientId = UAA_CLIENT_ID;
        String auditUaaClientSecret = UAA_CLIENT_SECRET;

        String systemIssuer = "http://www.uaa.com";
        String applicationClientId = "applicationClientId";
        String applicationClientSecret = "applicationClientSecret";
        String auditSystemClientId = "systemClientId";
        String auditSystemClientSecret = "auditSystemClientSecret";
        String tmsUrl = "http://tms.com";
        String tokenServiceUrl = "http://tokenService.com";
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("AUDIT_SERVICE_NAME")).thenReturn(auditServiceName);
        PowerMockito.when(System.getenv("AUDIT_UAA_URL")).thenReturn(auditUaaUrl);
        PowerMockito.when(System.getenv("AUDIT_UAA_CLIENT_ID")).thenReturn(auditUaaClientId);
        PowerMockito.when(System.getenv("AUDIT_UAA_CLIENT_SECRET")).thenReturn(auditUaaClientSecret);
        PowerMockito.when(System.getenv("AUDIT_SYSTEM_TRUSTED_ISSUER")).thenReturn(systemIssuer);
        PowerMockito.when(System.getenv("AUDIT_APP_NAME_CLIENT_ID")).thenReturn(applicationClientId);
        PowerMockito.when(System.getenv("AUDIT_APP_NAME_CLIENT_SECRET")).thenReturn(applicationClientSecret);
        PowerMockito.when(System.getenv("AUDIT_SYSTEM_CLIENT_ID")).thenReturn(auditSystemClientId);
        PowerMockito.when(System.getenv("AUDIT_SYSTEM_CLIENT_SECRET")).thenReturn(auditSystemClientSecret);
        PowerMockito.when(System.getenv("AUDIT_TMS_URL")).thenReturn(tmsUrl);
        PowerMockito.when(System.getenv("AUDIT_TOKEN_SERVICE_URL")).thenReturn(tokenServiceUrl);

        return RoutingAuditConfiguration.builder()
                .appNameConfig(AppNameConfig.builder()
                        .clientId(applicationClientId)
                        .clientSecret(applicationClientSecret)
                        .uaaUrl(systemIssuer)
                        .build())
                .sharedAuditConfig(SharedAuditConfig.builder()
                        .ehubUrl("ehub.asv-pr.ice.predix.io:443")
                        .ehubZoneId("eb7669e6-43f7-4e0f-9bcd-eb33d0ce3ca9")
                        .auditZoneId("83f75c67-85ac-4556-a2f7-989088d9ea80")
                        .uaaClientId(auditUaaClientId)
                        .uaaClientSecret(auditUaaClientSecret)
                        .uaaUrl(auditUaaUrl)
                        .tracingToken("token")
                        .tracingUrl("https://message-tracing.run.asv-pr.ice.predix.io/v1/checpoint")
                        .build())
                .systemConfig(SystemConfig.builder()
                        .clientId(auditSystemClientId)
                        .clientSecret(auditSystemClientSecret)
                        .tokenServiceUrl(tokenServiceUrl)
                        .tmsUrl(tmsUrl)
                        .build())
                .tenantAuditConfig(TenantAuditConfig.builder()
                        .auditServiceName(auditServiceName)
                        .bulkMode(true)
                        .maxNumberOfEventsInCachePerTenant(AbstractAuditConfiguration.DEFAULT_CACHE_SIZE)
                        .maxRetryCount(AbstractAuditConfiguration.DEFAULT_RETRY_COUNT)
                        .retryIntervalMillis(AbstractAuditConfiguration.DEFAULT_RETRY_INTERVAL_MILLIS)
                        .spaceName("dev")
                        .traceEnabled(true)
                        .tracingInterval(900000)
                        .build())
                .routingResourceConfig(RoutingResourceConfig.builder()
                        .numOfConnections(100)
                        .cacheRefreshPeriod(1000 * 60 * 2)
                        .connectionLifetime(1000 * 60 * 10)
                        .maxConcurrentAuditRequest(150000)
                        .sharedTenantCacheSize(10000)
                        .build())
                .build();
    }

    @Test
    @PrepareForTest( { EnvUtils.class })
    public void loadRoutingConfigurationFromVcapOptionalParamsPresent_ConfigurationIsCreated() throws IOException, VcapLoadException {
        setUpDefaultRoutingConfig();
        int num = 20000;
        String numAsAString = String.valueOf(num);
        long numAsALong = Long.valueOf(numAsAString);
        int retryCount = 4;
        vcapLoaderService.setMaxRetries(String.valueOf(retryCount));
        vcapLoaderService.setRetryIntervalMillis(numAsAString);
        vcapLoaderService.setMaxCachedEvents(numAsAString);
        PowerMockito.when(System.getenv("AUDIT_MAX_CACHED_EVENTS")).thenReturn(numAsAString);
        PowerMockito.when(System.getenv("AUDIT_ROUTING_NUM_OF_CONNECTIONS")).thenReturn(numAsAString);
        PowerMockito.when(System.getenv("AUDIT_ROUTING_SHARED_CACHE_SIZE")).thenReturn(numAsAString);
        PowerMockito.when(System.getenv("AUDIT_ROUTING_CACHE_REFRESH_PERIOD")).thenReturn(numAsAString);
        PowerMockito.when(System.getenv("AUDIT_ROUTING_CONNECTION_LIFETIME")).thenReturn(numAsAString);
        PowerMockito.when(System.getenv("AUDIT_ROUTING_MAX_CONCURRENT_REQUESTS")).thenReturn(numAsAString);

        RoutingAuditConfiguration configuration = vcapLoaderService.getRoutingConfigFromVcap();

        assertEquals(retryCount, configuration.getTenantAuditConfig().getMaxRetryCount());
        assertEquals(num, configuration.getTenantAuditConfig().getMaxNumberOfEventsInCachePerTenant());
        assertEquals(numAsALong, configuration.getTenantAuditConfig().getRetryIntervalMillis());
        assertEquals(num, configuration.getTenantAuditConfig().getMaxNumberOfEventsInCachePerTenant());
        assertEquals(num, configuration.getRoutingResourceConfig().getNumOfConnections());
        assertEquals(num, configuration.getRoutingResourceConfig().getMaxConcurrentAuditRequest());
        assertEquals(numAsALong, configuration.getRoutingResourceConfig().getConnectionLifetime());
        assertEquals(numAsALong, configuration.getRoutingResourceConfig().getCacheRefreshPeriod());
        assertEquals(num, configuration.getRoutingResourceConfig().getSharedTenantCacheSize());
    }

    @Test
    public void getConfigFromVcap_optionalParamsAreSet_valuesAreTakenFromEnv() throws IOException, VcapLoadException {
        String interval = "3000";
        String reconnectPolicy = "MANUAL";
        String maxRetries = "6";
        String maxCachedEvents = "15234";
        setValidVcapServicesAndApplication();
        vcapLoaderService.setMaxRetries(maxRetries);
        vcapLoaderService.setRetryIntervalMillis(interval);
        vcapLoaderService.setReconnectPolicy(reconnectPolicy);
        vcapLoaderService.setMaxCachedEvents(maxCachedEvents);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();

        assertNull(configFromVcap.getAuthToken());
        assertThat(configFromVcap.getRetryIntervalMillis(),is(3000L));
        assertThat(configFromVcap.getMaxRetryCount(),is(6));
        assertThat(configFromVcap.getReconnectMode(),is(ReconnectMode.MANUAL));
        assertThat(configFromVcap.getMaxNumberOfEventsInCache(),is(15234));
    }


    @Test(expected = VcapLoadException.class)
    public void getConfigFromVcap_optionalRetryIsInvalid_exception() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        String maxRetries = "retry";
        vcapLoaderService.setMaxRetries(maxRetries);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
    }


    @Test(expected = VcapLoadException.class)
    public void getConfigFromVcap_optionalRetryIsEmpty_exception() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        String maxRetries = "";
        vcapLoaderService.setMaxRetries(maxRetries);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
    }

    @Test(expected = VcapLoadException.class)
    public void getConfigFromVcap_reconnectIsInvalid_exception() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        String reconnect = "";
        vcapLoaderService.setReconnectPolicy(reconnect);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
    }

    @Test(expected = VcapLoadException.class)
    public void getConfigFromVcap_reconnectIsInvalid2_exception() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        String reconnect = "reconnect";
        vcapLoaderService.setReconnectPolicy(reconnect);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
    }

    @Test(expected = VcapLoadException.class)
    public void getConfigFromVcap_intervalIsInvalid_exception() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        String interval = "interval";
        vcapLoaderService.setRetryIntervalMillis(interval);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
    }

    @Test(expected = VcapLoadException.class)
    public void getConfigFromVcap_cacheSizeIsInvalid_exception() throws IOException, VcapLoadException {
        setValidVcapServicesAndApplication();
        String cacheSize = "12.5";
        vcapLoaderService.setRetryIntervalMillis(cacheSize);
        AuditConfiguration configFromVcap = vcapLoaderService.getConfigFromVcap();
    }

    private VcapServices getVcapServices(String vcapServices, String vcapApplication)
            throws IOException, VcapLoadException {
        String vcapServicesText = new String(Files.readAllBytes(Paths.get(vcapServices)));
        String vcapApplicationText = new String(Files.readAllBytes(Paths.get(vcapApplication)));
        VcapLoaderServiceImpl vcapLoaderServiceParam = new VcapLoaderServiceImpl(AUDIT_SERVICE, vcapServicesText);
        vcapLoaderServiceParam.setUaaUrl(UAA_URL);
        vcapLoaderServiceParam.setUaaClientId(UAA_CLIENT_ID);
        vcapLoaderServiceParam.setUaaClientSecret(UAA_CLIENT_SECRET);
        vcapLoaderServiceParam.setVcapApplicationEnv(vcapApplicationText);
        vcapLoaderServiceParam.getConfigFromVcap();
        return vcapLoaderServiceParam.getVcapServices();
    }
}
