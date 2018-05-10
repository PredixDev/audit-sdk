package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.ReconnectMode;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.config.vcap.VcapServices;
import com.ge.predix.audit.sdk.exception.VcapLoadException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Igor on 20/11/2016.
 */
public class VcapLoaderServiceTest {

    public static final String AUDIT_SERVICE = "audit-service";
    public static final String UAA_URL = "http://localhost:8090";
    public static final String UAA_CLIENT_ID = "admin";
    public static final String UAA_CLIENT_SECRET = "admin";

    private VcapLoaderServiceImpl vcapLoaderService;

    @Before
    public void setUp(){
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
