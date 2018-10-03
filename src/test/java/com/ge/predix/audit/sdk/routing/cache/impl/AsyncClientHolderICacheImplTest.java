package com.ge.predix.audit.sdk.routing.cache.impl;

import com.ge.predix.audit.sdk.AuditClientAsyncImpl;
import com.ge.predix.audit.sdk.config.TenantAuditConfig;
import com.ge.predix.audit.sdk.config.vcap.AuditServiceCredentials;
import com.ge.predix.audit.sdk.exception.TmsClientException;
import com.ge.predix.audit.sdk.exception.TokenException;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncShutdownHandler;
import com.ge.predix.audit.sdk.routing.cache.management.AuditCacheRefresher;
import com.ge.predix.audit.sdk.routing.tms.AuditTmsClient;
import com.ge.predix.audit.sdk.routing.tms.AuditTokenServiceClient;
import com.ge.predix.audit.sdk.routing.tms.TmsServiceInstance;
import com.ge.predix.audit.sdk.routing.tms.Token;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static com.ge.predix.audit.sdk.routing.RoutingTestHelper.generateHolders;
import static com.ge.predix.audit.sdk.util.ExceptionUtils.wrapCheckedException;
import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncClientHolderICacheImplTest {


    private static final int CACHE_REFRESH_RATE = 1000;
    private static final long EH_CONNECTION_LIFETIME = 500;
    private static final int EH_CONNECTION_LIMIT = 2;

    private AuditTokenServiceClient auditTokenServiceClient = mock(AuditTokenServiceClient.class);
    private AuditTmsClient auditTmsClient = mock(AuditTmsClient.class);
    private TenantAuditConfig tenantAuditConfig = TenantAuditConfig.builder()
            .auditServiceName("predix-audit")
            .spaceName("space")
            .traceEnabled(true)
            .tracingInterval(1000)
            .maxNumberOfEventsInCachePerTenant(2)
            .maxRetryCount(2)
            .retryIntervalMillis(20)
            .build();
    ExecutorService shutDownExecutorService = MoreExecutors.newDirectExecutorService();
    private AuditAsyncShutdownHandler auditAsyncShutdownHandler = spy(new AuditAsyncShutdownHandler( tenantAuditConfig,auditTokenServiceClient, shutDownExecutorService));

    private AuditCacheRefresher refresher = spy(new AuditCacheRefresher(auditTmsClient, auditTokenServiceClient));

    private AsyncClientHolderICacheImpl asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
            refresher, EH_CONNECTION_LIFETIME, EH_CONNECTION_LIMIT, CACHE_REFRESH_RATE);

    @Test
    public void shouldTryToRefreshTokenAndShutdownGracefullyWhenClientExpired() throws InterruptedException {
        //Insert one tenant that his token is valid for 1 second
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(1)).get(0);
        asyncClientHolderICache.put(holder.getTenantUuid(), holder);

        //Wait 1.5 second so this tenant will be expired
        sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

        //Verify client was shutdown as expected and token service tried to get a valid token
        verify(holder.getClient()).gracefulShutdown();
        verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
        verifyZeroInteractions(auditTmsClient);
        assertFalse(asyncClientHolderICache.get(holder.getTenantUuid()).isPresent());
    }

    @Test
    public void shouldShutdownGracefullyAuditClientWithoutTokenRefreshWhenCacheSizeHitsTheLimitAndTokenIsStillValid() {
        //Insert one tenant that his token is valid for 500 second
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(500)).get(0);
        asyncClientHolderICache.put(holder.getTenantUuid(), holder);

        //Insert more 2 tenants so the first tenant will be removed due to size limits
        List<String> validTenants = Arrays.asList("tenant2", "tenant3");
        asyncClientHolderICache.put(validTenants.get(0), mock(AuditAsyncClientHolder.class));
        asyncClientHolderICache.put(validTenants.get(1), mock(AuditAsyncClientHolder.class));

        //Verify client is shutdown and token service did not tried to get token since it's still valid
        verify(holder.getClient()).gracefulShutdown();
        verify(holder).refreshToken(auditTokenServiceClient, (tenantAuditConfig.getMaxRetryCount() + 1) * tenantAuditConfig.getRetryIntervalMillis());
        verify(auditTokenServiceClient, never()).getToken(holder.getTenantUuid());
        assertFalse(asyncClientHolderICache.get(holder.getTenantUuid()).isPresent());
        //Verify that other tenants are still remain in the cache
        assertTrue(asyncClientHolderICache.getAll().size() == 2);
    }

    @Test
    public void shouldShutdownGracefullyAuditClientAndRefreshTokenWhenCacheSizeHitsTheLimitAndTokenIsNotValid() {
        //Insert one tenant that his token is not valid
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(0)).get(0);
        asyncClientHolderICache.put(holder.getTenantUuid(), holder);

        //Insert more 2 tenants so the first tenant will be removed due to size limits
        List<String> validTenants = Arrays.asList("tenant2", "tenant3");
        asyncClientHolderICache.put(validTenants.get(0), mock(AuditAsyncClientHolder.class));
        asyncClientHolderICache.put(validTenants.get(1), mock(AuditAsyncClientHolder.class));

        //Verify client is shutdown and token service tried to get token since it's not valid
        verify(holder.getClient()).gracefulShutdown();
        verify(holder).refreshToken(auditTokenServiceClient, (tenantAuditConfig.getMaxRetryCount() + 1) * tenantAuditConfig.getRetryIntervalMillis());
        verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
        assertFalse(asyncClientHolderICache.get(holder.getTenantUuid()).isPresent());
        //Verify that other tenants are still remain in the cache
        assertTrue(asyncClientHolderICache.getAll().size() == 2);
    }

    @Test
    public void shouldTryRefreshTokenEveryCacheRefreshIntervalInCaseThatAuditServiceIsStillTheSameInTms () throws Exception {
        //Insert one tenant that his token is not valid
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(0)).get(0);
        String auditZoneId = holder.getAuditZoneId();
        //Set TMS and token service to return token and service instance with the same UUID for this tenant
        when(auditTmsClient.fetchServiceInstance(eq(holder.getTenantUuid()))).thenReturn(Optional.of(TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid(auditZoneId)
        .build()));
        String secondAccessToken = "SECOND_ACCESS_TOKEN";
        when(auditTokenServiceClient.getToken(eq(holder.getTenantUuid())))
                .thenReturn(new Token(secondAccessToken, "bearer", 2, "audit", "jti"));
        //Set conneciton lifetime to be longer then refresh rate
        int moreThanCacheRefreshRate = CACHE_REFRESH_RATE * 3;
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, moreThanCacheRefreshRate, EH_CONNECTION_LIMIT, CACHE_REFRESH_RATE);
        asyncClientHolderICache.put(holder.getTenantUuid(), holder);

        //Wait for handler to invoke refresh
        sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

        verify(auditTmsClient).fetchServiceInstance(holder.getTenantUuid());
        verify(holder).refreshToken(auditTokenServiceClient, CACHE_REFRESH_RATE);
        verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
        assertTrue(asyncClientHolderICache.get(holder.getTenantUuid()).isPresent());
        verify(holder.getClient()).setAuthToken(secondAccessToken);
    }


    @Test
    public void shouldContinueToTryRefreshTokenEveryIntervalWhenAuditServiceIsStillInTmsAndOneTenantIsFailedToFetchToken ()
            throws InterruptedException{
       //Generate 3 clients with invalid token.
       List<AuditAsyncClientHolder> holders = generateHolders(Arrays.asList(0,0,0));

        //Set conneciton lifetime to be longer then refresh rate
        int moreThanCacheRefreshRate = CACHE_REFRESH_RATE * 3;
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, moreThanCacheRefreshRate, holders.size(), CACHE_REFRESH_RATE);

        //Set TMS and token service to return token and service instance with the same UUID for this tenant except tenant2 which will throw exception when getting Token
       //Insert the holder to the cache
       String failedTenant = holders.get(1).getTenantUuid();
       String accessToken = "kuku";
       holders.forEach(holder -> {
           String auditZoneId = holder.getAuditZoneId();
           when(auditTmsClient.fetchServiceInstance(eq(holder.getTenantUuid()))).
                   thenReturn(Optional.of(TmsServiceInstance.<AuditServiceCredentials>builder()
                    .serviceInstanceUuid(auditZoneId)
                   .build()));
           if (holder.getTenantUuid().equals(failedTenant)) { //the second tenant will fail to get token
               when(auditTokenServiceClient.getToken(eq(failedTenant))).thenThrow(new TokenException("token could not be refreshed!"));
           } else {
               Token token = new Token(accessToken, "bearer", 200, "audit.zones", "jti");
               when(auditTokenServiceClient.getToken(eq(holder.getTenantUuid()))).thenReturn(token);
           }
           asyncClientHolderICache.put(holder.getTenantUuid(), holder);
       });

       //Wait for handler to invoke refresh
       sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

       holders.forEach( holder -> {
           verify(auditTmsClient).fetchServiceInstance(holder.getTenantUuid());
           verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
           verify(holder).refreshToken(auditTokenServiceClient, CACHE_REFRESH_RATE);
           if (holder.getTenantUuid().equals(failedTenant)) {
               wrapCheckedException(() -> verify(holder.getClient(), never()).setAuthToken(anyString()));
           }
           else {
               wrapCheckedException(() -> verify(holder.getClient()).setAuthToken(accessToken));
           }

       });
       assertEquals(3, asyncClientHolderICache.getAll().size());
   }

    @Test
    public void shouldContinueToTryRefreshTokenEveryIntervalWhenAuditServiceIsStillInTmsAndOneTenantIsFailedToHitTms ()
            throws InterruptedException{
        //Generate 3 clients with invalid token.
        List<AuditAsyncClientHolder> holders = generateHolders(Arrays.asList(0,0,0));

        //Set conneciton lifetime to be longer then refresh rate
        int moreThanCacheRefreshRate = CACHE_REFRESH_RATE * 3;
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, moreThanCacheRefreshRate, holders.size(), CACHE_REFRESH_RATE);

        //Set TMS and token service to return token and service instance with the same UUID for this tenant except tenant2 which will throw exception when getting Token
        //Insert the holder to the cache
        String failedTenant = holders.get(1).getTenantUuid();
        String accessToken = "kuku";
        holders.forEach(holder -> {
            Token token = new Token(accessToken, "bearer", 200, "audit.zones", "jti");
            when(auditTokenServiceClient.getToken(eq(holder.getTenantUuid()))).thenReturn(token);
            if (holder.getTenantUuid().equals(failedTenant)) { //the second tenant will fail to get token
                when(auditTmsClient.fetchServiceInstance(eq(failedTenant))).thenThrow(new TmsClientException("Could not hit TMS for tenant" +failedTenant));
            } else {
                String auditZoneId = holder.getAuditZoneId();
                when(auditTmsClient.fetchServiceInstance(eq(holder.getTenantUuid()))).thenReturn(Optional.of(TmsServiceInstance.<AuditServiceCredentials>builder()
                        .serviceInstanceUuid(auditZoneId)
                        .build()));
            }
            asyncClientHolderICache.put(holder.getTenantUuid(), holder);
        });

        //Wait for handler to invoke refresh
        sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

        holders.forEach( holder -> {
            verify(auditTmsClient).fetchServiceInstance(holder.getTenantUuid());
            if (holder.getTenantUuid().equals(failedTenant)) {
                wrapCheckedException(() -> verify(holder.getClient(), never()).setAuthToken(anyString()));
            }
            else {
                verify(holder).refreshToken(auditTokenServiceClient, CACHE_REFRESH_RATE);
                verify(auditTmsClient).fetchServiceInstance(holder.getTenantUuid());
                verify(auditTokenServiceClient).getToken(holder.getTenantUuid());
                wrapCheckedException(() -> verify(holder.getClient()).setAuthToken(accessToken));
            }
        });
        assertEquals(3, asyncClientHolderICache.getAll().size());
    }

    @Test
    public void shouldShutdownAuditClientAndRemoveFromCacheWhenTenantHasDifferentAuditZoneIdInTms() throws Exception {
        //Generate holder
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(0)).get(0);
        int moreThanCacheRefreshRate = CACHE_REFRESH_RATE * 3;
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, moreThanCacheRefreshRate, 1, CACHE_REFRESH_RATE);

        //Tell TMS to return different audit instance;
        when(auditTmsClient.fetchServiceInstance(eq(holder.getTenantUuid()))).thenReturn(Optional.of(TmsServiceInstance.<AuditServiceCredentials>builder()
                .serviceInstanceUuid("other")
                .build()));
        //Insert the holder to the cache
        asyncClientHolderICache.put(holder.getTenantUuid(), holder);

        //Wait for handler to invoke refresh
        sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

        verify(auditTmsClient).fetchServiceInstance(holder.getTenantUuid());
        verify(auditTokenServiceClient, never()).getToken(anyString());
        wrapCheckedException(() -> verify(holder.getClient(), never()).setAuthToken(anyString()));
        verify(holder.getClient()).shutdown();
        verify(holder.getClient(), never()).gracefulShutdown();
        assertTrue(asyncClientHolderICache.getAll().isEmpty());
    }

    @Test
    public void shouldShutdownAuditClientAndRemoveFromCacheWhenTenantHasNoAuditZoneIdInTms() throws Exception {
        //Generate holder
        AuditAsyncClientHolder holder = generateHolders(Collections.singletonList(0)).get(0);
        int moreThanCacheRefreshRate = CACHE_REFRESH_RATE * 3;
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, moreThanCacheRefreshRate, 1, CACHE_REFRESH_RATE);

        //Tell TMS to return null audit instance
        when(auditTmsClient.fetchServiceInstance(eq(holder.getTenantUuid()))).thenReturn(Optional.empty());
        //Insert the holder to the cache
        asyncClientHolderICache.put(holder.getTenantUuid(), holder);

        //Wait for handler to invoke refresh
        sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

        verify(auditTmsClient).fetchServiceInstance(holder.getTenantUuid());
        verify(auditTokenServiceClient, never()).getToken(anyString());
        wrapCheckedException(() -> verify(holder.getClient(), never()).setAuthToken(anyString()));
        verify(holder.getClient()).shutdown();
        verify(holder.getClient(), never()).gracefulShutdown();
        assertTrue(asyncClientHolderICache.getAll().isEmpty());
    }

    @Test
    public void shouldShutdownImidiatlyAllClientsWhenTenantHasDifferentAuditInTmsEvenIfOneOfThemFailed() throws Exception {
        //Generate holder
        List<AuditAsyncClientHolder> holders = generateHolders(Arrays.asList(0,0));
        //Set the first holder to throw exception some exception
        AuditClientAsyncImpl clientAsync = holders.get(0).getClient();
        when(holders.get(0).getClient()).thenThrow(new RuntimeException("not allowed to get this audit client!"));
        //Tell TMS to return null audit instance for all tenants
        when(auditTmsClient.fetchServiceInstance(any())).thenReturn(Optional.empty());
        //Insert the holder to the cache
        int moreThanCacheRefreshRate = CACHE_REFRESH_RATE * 3;
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, moreThanCacheRefreshRate, holders.size(), CACHE_REFRESH_RATE);
        holders.forEach(holder -> asyncClientHolderICache.put(holder.getTenantUuid(), holder));

        //Wait for handler to invoke refresh
        sleep(CACHE_REFRESH_RATE + CACHE_REFRESH_RATE/2);

        holders.forEach(holder -> verify(auditTmsClient).fetchServiceInstance(holders.get(0).getTenantUuid()));

        //Verify cache is empty and TMS returned ampty clients
        verify(auditTmsClient, times(2)).fetchServiceInstance(any());
        verify(auditTokenServiceClient, never()).getToken(anyString());
        assertTrue(asyncClientHolderICache.getAll().isEmpty());
        //Verify second client is shut down
        verify(holders.get(1).getClient()).shutdown();
        verify(holders.get(1).getClient(), never()).gracefulShutdown();
        //Verify first client was not shut down
        verify(clientAsync, never()).gracefulShutdown();

    }

    @Test
    public void shutdownTest() throws Exception {
        //Generate holders
        List<AuditAsyncClientHolder> holders = generateHolders(Arrays.asList(0,0));
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, EH_CONNECTION_LIFETIME, holders.size(), CACHE_REFRESH_RATE);
        holders.forEach(holder -> asyncClientHolderICache.put(holder.getTenantUuid(), holder));

        asyncClientHolderICache.shutdown();

        holders.forEach(holder -> {
            verify(holder.getClient()).shutdown();
        });
        assertTrue(shutDownExecutorService.isShutdown());
    }

    @Test
    public void gracefulShutdownTest() throws Exception {
        //Generate holders
        List<AuditAsyncClientHolder> holders = generateHolders(Arrays.asList(0,0));
        asyncClientHolderICache = new AsyncClientHolderICacheImpl(auditAsyncShutdownHandler,
                refresher, EH_CONNECTION_LIFETIME, holders.size(), CACHE_REFRESH_RATE);
        holders.forEach(holder -> asyncClientHolderICache.put(holder.getTenantUuid(), holder));

        asyncClientHolderICache.gracefulShutdown();

        holders.forEach(holder -> {
            verify(holder.getClient()).gracefulShutdown();
        });
        assertTrue(shutDownExecutorService.isShutdown());
    }


}