package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.eventhub.EventHubClientException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Martin Saad on 2/13/2017.
 */
public class AuditClientTest {

    private static AuditConfiguration validConfiguration;
    private static AuditConfiguration validSyncConfiguration;
    private static AuditConfiguration validConfigurationWithAuthToken;
    private static AuditConfiguration invalidRetryIntervalConfiguration;
    private static AuditConfiguration invalidRetryValueConfiguration;
    private static AuditConfiguration invalidCacheSizeConfiguration;
    private static AuditConfiguration validConfigurationNoTracingData;
    private static TestHelper cb;



    AuditClient auditClient ;

    @BeforeClass
    public static void init(){

        validConfiguration = AuditConfiguration.builder()
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

        validConfigurationWithAuthToken = AuditConfiguration.builderWithAuthToken()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .authToken("token")
                .tracingInterval(300)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .bulkMode(false)
                .build();

        validConfigurationNoTracingData = AuditConfiguration.builderWithAuthToken()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .traceEnabled(false)
                .bulkMode(false)
                .build();

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


        cb = new TestHelper();
    }

    @Test(expected = AuditException.class)
    public void createClientWithInvalidRetryIntervalTest() throws EventHubClientException, AuditException {
       new AuditClient(invalidRetryIntervalConfiguration, cb);
    }

    @Test(expected = AuditException.class)
    public void createClientWithInvalidRetryCountConfigurationTest() throws EventHubClientException, AuditException {
        new AuditClient(invalidRetryValueConfiguration, cb);
    }

    @Test(expected = AuditException.class)
    public void createClientWithInvalidCacheSizeConfigurationTest() throws EventHubClientException, AuditException {
      new AuditClient(invalidCacheSizeConfiguration, cb);
    }

    @Test (expected = AuditException.class)
    public void createClientWithNoConfigurationTest() throws EventHubClientException, AuditException {
       new AuditClient(null, cb);
    }

    @Test
    public void createClientWithAuthTokenAuthenticationTest() throws EventHubClientException, AuditException {
        auditClient = new AuditClient(validConfigurationWithAuthToken, cb);
        assertThat(auditClient.getAuditConfiguration().getAuthenticationMethod(), is(AuthenticationMethod.AUTH_TOKEN));
    }

    @Test
    public void createClientWithUAAUserTest() throws EventHubClientException, AuditException {
        auditClient = new AuditClient(validConfiguration, cb);
        assertThat(auditClient.getAuditConfiguration().getAuthenticationMethod(), is(AuthenticationMethod.UAA_USER_PASS));
    }


    @Test(expected = IllegalStateException.class)
    public void startTracingFlow_noTracingConfiguration_throws() throws EventHubClientException, AuditException {
        auditClient = new AuditClient(validConfigurationNoTracingData, cb);
        auditClient.trace();
    }

    @Test(expected = AuditException.class)
    public void startTracingFlowIntervalZeroTest() throws EventHubClientException, AuditException {
        AuditConfiguration validConfigurationInvalidTracingInterval = AuditConfiguration.builderWithAuthToken()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .authToken("token")
                .tracingInterval(0)
                .traceEnabled(true)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .bulkMode(false)
                .build();
        auditClient = new AuditClient(validConfigurationInvalidTracingInterval, cb);
    }

    @Test(expected = AuditException.class)
    public void startTracingUrlIsEmptyTest() throws EventHubClientException, AuditException {
        AuditConfiguration validConfigurationInvalidTracingInterval = AuditConfiguration.builderWithAuthToken()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .authToken("token")
                .tracingInterval(10000)
                .tracingUrl("")
                .traceEnabled(true)
                .tracingToken("token")
                .bulkMode(false)
                .build();
        auditClient = new AuditClient(validConfigurationInvalidTracingInterval, cb);
    }

    @Test
    public void startTracingIsNotEnabled_noExceptionONConfiguration() throws EventHubClientException, AuditException {
        AuditConfiguration validConfigurationInvalidTracingInterval = AuditConfiguration.builderWithAuthToken()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .authToken("token")
                .tracingInterval(0)
                .tracingUrl("")
                .traceEnabled(false)
                .bulkMode(false)
                .build();
        auditClient = new AuditClient(validConfigurationInvalidTracingInterval, cb);
    }


}
