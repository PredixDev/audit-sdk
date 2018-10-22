package com.ge.predix.audit.sdk.routing.cache;

import com.ge.predix.audit.sdk.*;
import com.ge.predix.audit.sdk.config.*;
import com.ge.predix.audit.sdk.config.vcap.AuditServiceCredentials;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.RoutingAuditException;
import com.ge.predix.audit.sdk.exception.TmsClientException;
import com.ge.predix.audit.sdk.exception.TokenException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.routing.RoutingTestHelper;
import com.ge.predix.audit.sdk.routing.cache.impl.AsyncClientHolderICacheImpl;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.cache.impl.CommonClientInterfaceICacheImpl;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncClientFactory;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncShutdownHandler;
import com.ge.predix.audit.sdk.routing.cache.management.AuditCacheRefresher;
import com.ge.predix.audit.sdk.routing.tms.*;
import com.ge.predix.eventhub.EventHubClientException;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.ge.predix.audit.sdk.routing.RoutingTestHelper.generateHolders;
import static com.ge.predix.audit.sdk.routing.RoutingTestHelper.generateToken;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class TenantCacheProxyTest {

    ICache<String, AuditAsyncClientHolder> dedicatedClients ;
    ICache<String, CommonClientInterface> sharedClients;
    AuditTokenServiceClient auditTokenServiceClient = mock(AuditTokenServiceClient.class);
    AuditTmsClient auditTmsClient = mock(AuditTmsClient.class);
    TokenClient tokenClient = mock(TokenClient.class);
    RoutingAuditConfiguration configuration = RoutingAuditConfiguration.builder()
            .appNameConfig(AppNameConfig.builder()
                        .clientSecret("a")
                        .clientId("a")
                        .uaaUrl("a")
                        .build())
            .sharedAuditConfig(SharedAuditConfig.builder()
                        .auditZoneId("b")
                        .ehubUrl("eh:8080")
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
    //AppNameClient appNameClient = new AppNameClient(tokenClient, configuration.getAppNameConfig().getAppNamePrefix());
    RoutingTestHelper<AuditEventV2> testHelper = new RoutingTestHelper<>();

    AuditAsyncClientFactory<AuditEventV2> factory;
    TenantCacheProxy proxy ;

    @Before
    public void init() {
        //LoggerUtils.setLoggersLogLevel(Level.INFO);
        when(tokenClient.getToken(false)).thenReturn(new Token("access", "bearer", 100, "stuf.app.kuku", "jti"));
        sharedClients = spy(new CommonClientInterfaceICacheImpl(5000, 100));
        dedicatedClients = spy(new AsyncClientHolderICacheImpl(new AuditAsyncShutdownHandler(configuration.getTenantAuditConfig(), auditTokenServiceClient, MoreExecutors.newDirectExecutorService()), mock(AuditCacheRefresher.class), 5000, 5000, 5000));
        factory = spy(new AuditAsyncClientFactory<>(testHelper, configuration, auditTmsClient, auditTokenServiceClient, sharedClients, dedicatedClients));
        proxy = new TenantCacheProxy(dedicatedClients, sharedClients, auditTokenServiceClient, factory, 1000);
    }

    @Test
    public void getDefaultClient() {

       assertEquals(proxy.getDefaultClient(), factory.getSharedClient());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenGetDefaultClientThrowsException() {
        when(factory.getSharedClient()).thenThrow(new RuntimeException("could not get shared cient"));
        proxy.getDefaultClient();
    }

    @Test
    public void getClientForSharedTenantReturnSharedClient() {
        String tnt = "tnt";
        CommonClientInterface sharedClient = factory.getSharedClient();
        sharedClients.put(tnt, sharedClient);

        assertEquals(sharedClient, proxy.getClientFor(tnt));
    }

    @Test
    public void getClientForSharedTenantAndTenantNotInCacheSharedCacheIsUpdated() {
        String tnt = "tnt";
        when(auditTmsClient.fetchServiceInstance(tnt)).thenReturn(Optional.empty());

        CommonClientInterface commonClientInterface = proxy.getClientFor(tnt);

        assertEquals(factory.getSharedClient(), commonClientInterface);

        verify(sharedClients).put(tnt, factory.getSharedClient());
        assertTrue(sharedClients.get(tnt).isPresent());
        assertEquals(factory.getSharedClient(), sharedClients.get(tnt).get());
        assertEquals(1, sharedClients.getAll().size());
        assertTrue(dedicatedClients.getAll().isEmpty());
    }

    @Test
    public void getClientForSharedTenantAndTenantNotInCacheAndTmsClientThrowsExceptionExceptionIsBabbledUp() {
        String tnt = "tnt";
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        when(auditTmsClient.fetchServiceInstance(tnt)).thenThrow(new TmsClientException("could not fetch instance from TMS!"));

        assertThatThrownBy(() -> proxy.getClientFor(tnt)).isInstanceOf(TmsClientException.class);

        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
    }


    @Test
    public void getClientForDedicatedTenantAndTokenIsValidReturnDedicatedClient() {
        String tnt = "tnt";
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(100)).get(0);
        dedicatedClients.put(tnt, holder);

        CommonClientInterface commonClientInterface = proxy.getClientFor(tnt);

        assertEquals(holder.getClient(), commonClientInterface);
        verifyZeroInteractions(auditTmsClient, auditTokenServiceClient, factory);
    }

    @Test
    public void getClientForDedicatedTenantAndTokenIsNotValidReturnDedicatedClientAndTokenIsRefreshed() throws EventHubClientException, AuditException {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(0)).get(0);
        Token newToken = new Token("access", "token", 100, "kuku", "jti");
        when(auditTokenServiceClient.getToken(eq(holder.getTenantUuid()))).thenReturn(newToken);
        dedicatedClients.put(holder.getTenantUuid(), holder);

        CommonClientInterface commonClientInterface = proxy.getClientFor(holder.getTenantUuid());

        assertEquals(holder.getClient(), commonClientInterface);
        verify(holder).refreshToken(auditTokenServiceClient, 1000);
        verify(holder.getClient()).setAuthToken(newToken.getAccessToken());
        verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
        verify(dedicatedClients).get(holder.getTenantUuid());
        verifyZeroInteractions(auditTmsClient, factory, auditTokenServiceClient);
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void TenantIsInCacheAndAuthorizationErrorWasTrrigeredTokenIsRefreshed() throws EventHubClientException, AuditException {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String tenantId = "tenant";
        Token newToken = new Token("access", "token", 100, "kuku", "jti");
        when(auditTokenServiceClient.getToken(eq(tenantId))).thenReturn(newToken);
        AuditServiceCredentials credentials = AuditServiceCredentials.builder()
                .auditPubClientScope(Collections.singletonList("scope"))
                .auditQueryApiScope("1")
                .auditQueryApiUrl("http://query.com")
                .eventHubUri("a:123")
                .eventHubZoneId("zoneId")
                .tracingInterval(1000)
                .tracingToken("1234=")
                .tracingUrl("http://tracing.com")
                .build();
        TmsServiceInstance serviceInstance = TmsServiceInstance.builder()
                .serviceInstanceName("predix-audit")
                .serviceInstanceUuid("12345")
                .canonicalServiceName("predix-audit")
                .serviceName("a")
                .createdBy("kuku")
                .createdOn(System.currentTimeMillis())
                .managed(false)
                .status("created")
                .credentials(credentials)
                .build();
        when(auditTmsClient.fetchServiceInstance(tenantId)).thenReturn(Optional.of(serviceInstance));
        factory.createClient(tenantId);
        AuditAsyncClientHolder holder = dedicatedClients.get(tenantId).orElseThrow(()->new RuntimeException("holder should exists!"));
        holder.getClient().getCallback().onClientError(ClientErrorCode.AUTHENTICATION_FAILURE, "tokenIsBad");

        verify(auditTokenServiceClient, times(2)).getToken(holder.getTenantUuid());
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void TenantIsInCacheAndAuthorizationErrorWasTrrigeredTokenIsRefreshedAndTokenServiceThrowsExceptionExceptionIsSwallowed() throws EventHubClientException, AuditException {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String tenantId = "tenant";
        Token newToken = new Token("access", "token", 100, "kuku", "jti");
        when(auditTokenServiceClient.getToken(eq(tenantId))).thenReturn(newToken).thenThrow(new RuntimeException("cannot fetch token!"));
        AuditServiceCredentials credentials = AuditServiceCredentials.builder()
                .auditPubClientScope(Collections.singletonList("scope"))
                .auditQueryApiScope("1")
                .auditQueryApiUrl("http://query.com")
                .eventHubUri("a:123")
                .eventHubZoneId("zoneId")
                .tracingInterval(1000)
                .tracingToken("1234=")
                .tracingUrl("http://tracing.com")
                .build();
        TmsServiceInstance serviceInstance = TmsServiceInstance.builder()
                .serviceInstanceName("predix-audit")
                .serviceInstanceUuid("12345")
                .canonicalServiceName("predix-audit")
                .serviceName("a")
                .createdBy("kuku")
                .createdOn(System.currentTimeMillis())
                .managed(false)
                .status("created")
                .credentials(credentials)
                .build();
        when(auditTmsClient.fetchServiceInstance(tenantId)).thenReturn(Optional.of(serviceInstance));
        factory.createClient(tenantId);
        AuditAsyncClientHolder holder = dedicatedClients.get(tenantId).orElseThrow(()->new RuntimeException("holder should exists!"));
        holder.getClient().getCallback().onClientError(ClientErrorCode.AUTHENTICATION_FAILURE, "tokenIsBad");

        verify(auditTokenServiceClient, times(2)).getToken(holder.getTenantUuid());
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void getClientForDedicatedTenantAndTokenIsNotValidReturnDedicatedClientAndTokenServiceThrowsExceptionExceptionIsReturned() {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(0)).get(0);
        when(auditTokenServiceClient.getToken(eq(holder.getTenantUuid()))).thenThrow(new TokenException("kuku"));
        dedicatedClients.put(holder.getTenantUuid(), holder);

        assertThatThrownBy(() -> proxy.getClientFor(holder.getTenantUuid())).isInstanceOf(TokenException.class);

        verify(holder).refreshToken(auditTokenServiceClient, 1000);
        verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
        verify(dedicatedClients).get(holder.getTenantUuid());
        verifyZeroInteractions(auditTmsClient, factory);
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void getClientForDedicatedTenantAndTenantNotInCacheCacheIsUpdated() {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String tenant = "tnt";
        String auditZoneId = "auditZoneId";
        TmsServiceInstance<AuditServiceCredentials> instance = TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid(auditZoneId)
                .credentials(AuditServiceCredentials.builder()
                        .tracingUrl("http://tracing.com")
                        .tracingInterval(1000)
                        .auditQueryApiScope("api.scope")
                        .auditPubClientScope(Arrays.asList("1","2"))
                        .eventHubUri("localhost:8080")
                        .eventHubZoneId("ehubZoneId")
                        .auditQueryApiUrl("http://api.com")
                        .build())
                .build();
        when(auditTmsClient.fetchServiceInstance(eq(tenant))).thenReturn(Optional.of(instance));
        Token token = generateToken(0, 1);
        when(auditTokenServiceClient.getToken(eq(tenant))).thenReturn(token);

        CommonClientInterface clientInterface = proxy.getClientFor(tenant);

        verify(auditTokenServiceClient).getToken(tenant);
        verify(dedicatedClients).put(tenant, new AuditAsyncClientHolder((AuditClientAsyncImpl) clientInterface, tenant, instance.getServiceInstanceUuid(), token));
        assertTrue(dedicatedClients.get(tenant).isPresent());
        assertEquals(clientInterface, dedicatedClients.get(tenant).get().getClient());
        assertTrue(sharedClients.getAll().isEmpty());

    }

    @Test
    public void getClientForDedicatedTenantAndTenantNotInCacheAndTmsThrowExceptionExceptionIsBubledUp() {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String tenant = "tnt";
        when(auditTmsClient.fetchServiceInstance(eq(tenant))).thenThrow(new TmsClientException("Could not fetch instance for tenant!"));

        assertThatThrownBy(() -> proxy.getClientFor(tenant)).isInstanceOf(TmsClientException.class);

        verifyZeroInteractions(auditTokenServiceClient);
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void getClientForDedicatedTenantAndTenantNotInCacheAndTokenServiceThrowExceptionExceptionIsBubledUp() {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String tenant = "tnt";
        String auditZoneId = "auditZoneId";
        TmsServiceInstance<AuditServiceCredentials> instance = TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid(auditZoneId)
                .credentials(AuditServiceCredentials.builder()
                        .tracingUrl("http://tracing.com")
                        .tracingInterval(1000)
                        .auditQueryApiScope("api.scope")
                        .auditPubClientScope(Arrays.asList("1","2"))
                        .eventHubUri("localhost:8080")
                        .eventHubZoneId("ehubZoneId")
                        .auditQueryApiUrl("http://api.com")
                        .build())
                .build();
        when(auditTmsClient.fetchServiceInstance(eq(tenant))).thenReturn(Optional.of(instance));
        when(auditTokenServiceClient.getToken(eq(tenant))).thenThrow(new TokenException("could not fetch token for audit client"));

        assertThatThrownBy(()-> proxy.getClientFor(tenant)).isInstanceOf(TokenException.class);

        verify(auditTokenServiceClient).getToken(tenant);
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void getClientForDedicatedTenantAndTenantNotInCacheAndAuditClientThrowExceptionExceptionIsBubledUp() {
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String tenant = "tnt";
        String auditZoneId = "auditZoneId";
        TmsServiceInstance<AuditServiceCredentials> instance = TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid(auditZoneId)
                .credentials(AuditServiceCredentials.builder()
                        .tracingUrl("http://tracing.com")
                        .tracingInterval(1000)
                        .auditQueryApiScope("api.scope")
                        .auditPubClientScope(Arrays.asList("1","2"))
                        .eventHubUri("localhost:8080")
                        .eventHubZoneId(null)//will fail on EventHubClientException
                        .auditQueryApiUrl("http://api.com")
                        .build())
                .build();
        when(auditTmsClient.fetchServiceInstance(eq(tenant))).thenReturn(Optional.of(instance));
        Token token = generateToken(0, 1);
        when(auditTokenServiceClient.getToken(eq(tenant))).thenReturn(token);

        assertThatThrownBy( ()-> proxy.getClientFor(tenant)).isInstanceOf(RoutingAuditException.class);

        verify(auditTokenServiceClient).getToken(tenant);
        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
    }

    @Test
    public void shutdownClosingSharedAuditClient() {
        AuditEventV2 sharedEvent = AuditEventV2.builder()
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();

        assertEquals(AuditClientState.CONNECTED, factory.getSharedClient().getAuditClientState());
        factory.getSharedClient().audit(sharedEvent);
        assertEquals(AuditClientState.DISCONNECTED, factory.getSharedClient().getAuditClientState());

        proxy.shutDown();

        assertEquals(AuditClientState.SHUTDOWN, factory.getSharedClient().getAuditClientState());
        assertEquals(sharedEvent, testHelper.getKpis(null).getLastFailureEvent());
        assertEquals(1, testHelper.getKpis(null).getFailureCount().get());
    }

    @Test
    public void shutdownClosingDedicatedAuditClient() throws AuditException {
        String tenant = "tnt";
        AuditEventV2 dedicated = AuditEventV2.builder()
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .tenantUuid(tenant)
                .build();

        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String auditZoneId = "auditZoneId";
        TmsServiceInstance<AuditServiceCredentials> instance = TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid(auditZoneId)
                .credentials(AuditServiceCredentials.builder()
                        .tracingUrl("http://tracing.com")
                        .tracingInterval(1000)
                        .auditQueryApiScope("api.scope")
                        .auditPubClientScope(Arrays.asList("1","2"))
                        .eventHubUri("localhost:8080")
                        .eventHubZoneId("ehubZoneId")
                        .auditQueryApiUrl("http://api.com")
                        .build())
                .build();
        when(auditTmsClient.fetchServiceInstance(eq(tenant))).thenReturn(Optional.of(instance));
        Token token = generateToken(0, 1);
        when(auditTokenServiceClient.getToken(eq(tenant))).thenReturn(token);
        CommonClientInterface clientInterface = proxy.getClientFor(tenant);
        assertEquals(AuditClientState.CONNECTED, clientInterface.getAuditClientState());
        clientInterface.audit(dedicated);

        proxy.shutDown();

        assertEquals(AuditClientState.SHUTDOWN, clientInterface.getAuditClientState());
        assertEquals(AuditClientState.SHUTDOWN, factory.getSharedClient().getAuditClientState());
        assertEquals(dedicated, testHelper.getKpis(tenant).getLastFailureEvent());
        assertEquals(1, testHelper.getKpis(tenant).getFailureCount().get());
    }
    
    @Test
    public void gracefulShutdown() throws Exception {
        String tenant = "tnt";
        AuditEventV2 dedicated = AuditEventV2.builder()
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .tenantUuid(tenant)
                .build();

        assertTrue(dedicatedClients.getAll().isEmpty());
        assertTrue(sharedClients.getAll().isEmpty());
        String auditZoneId = "auditZoneId";
        TmsServiceInstance<AuditServiceCredentials> instance = TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid(auditZoneId)
                .credentials(AuditServiceCredentials.builder()
                        .tracingUrl("http://tracing.com")
                        .tracingInterval(1000)
                        .auditQueryApiScope("api.scope")
                        .auditPubClientScope(Arrays.asList("1","2"))
                        .eventHubUri("localhost:8080")
                        .eventHubZoneId("ehubZoneId")
                        .auditQueryApiUrl("http://api.com")
                        .build())
                .build();
        when(auditTmsClient.fetchServiceInstance(eq(tenant))).thenReturn(Optional.of(instance));
        Token token = generateToken(0, 100000);
        when(auditTokenServiceClient.getToken(eq(tenant))).thenReturn(token);
        CommonClientInterface clientInterface = proxy.getClientFor(tenant);
        assertEquals(AuditClientState.CONNECTED, clientInterface.getAuditClientState());
        clientInterface.audit(dedicated);

        proxy.gracefulShutdown();

        assertEquals(AuditClientState.SHUTDOWN, clientInterface.getAuditClientState());
        assertEquals(AuditClientState.SHUTDOWN, factory.getSharedClient().getAuditClientState());
        assertEquals(dedicated, testHelper.getKpis(tenant).getLastFailureEvent());
        assertEquals(1, testHelper.getKpis(tenant).getFailureCount().get());
    }
}