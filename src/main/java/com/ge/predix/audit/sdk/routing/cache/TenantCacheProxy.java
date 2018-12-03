package com.ge.predix.audit.sdk.routing.cache;

import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncClientFactory;
import com.ge.predix.audit.sdk.routing.tms.AuditTokenServiceClient;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Optional;

@AllArgsConstructor
public class TenantCacheProxy<T extends AuditEvent> {

    private static CustomLogger log = LoggerUtils.getLogger(TenantCacheProxy.class.getName());

    private final ICache<String, AuditAsyncClientHolder<T>> dedicatedClients;
    private final ICache<String, CommonClientInterface<T>> sharedTenants;
    private final AuditTokenServiceClient tokenServiceClient;
    private final AuditAsyncClientFactory<T> auditClientFactory;
    private final long tokenRefreshThreshold;

    public void shutDown() {
        log.warning("Shutting down TenantCacheProxy");
        sharedTenants.shutdown();
        dedicatedClients.shutdown();
        auditClientFactory.getSharedClient().shutdown();
    }

    public void gracefulShutdown() throws Exception {
        log.warning("Shutting down gracefully TenantCacheProxy");
        sharedTenants.gracefulShutdown();
        dedicatedClients.gracefulShutdown();
        auditClientFactory.getSharedClient().gracefulShutdown();
    }

    public CommonClientInterface<T> getDefaultClient() {
        return this.auditClientFactory.getSharedClient();
    }

    public CommonClientInterface<T> getClientFor(@NonNull String tenantUuid) {
        return getFromCache(tenantUuid)
                .orElseGet( () -> auditClientFactory.createClient(tenantUuid));
    }

    private Optional<CommonClientInterface<T>> getFromCache(@NonNull String tenantUuid) {
        log.info(String.format("checking if tenantUuid %s is in cache", tenantUuid));
        return sharedTenants.get(tenantUuid)
                .map(Optional::of)
                .orElseGet(() -> getDedicatedClient(tenantUuid));
    }

    private Optional<CommonClientInterface<T>> getDedicatedClient(String tenantUuid) {
        log.info(String.format("checking if tenantUuid %s is in dedicated cache", tenantUuid));
        return dedicatedClients.get(tenantUuid)
                .map(cacheObject -> cacheObject.refreshToken(tokenServiceClient, tokenRefreshThreshold))
                .map(AuditAsyncClientHolder::getClient);
    }
}
