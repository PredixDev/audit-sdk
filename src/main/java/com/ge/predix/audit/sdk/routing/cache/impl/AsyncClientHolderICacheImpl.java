package com.ge.predix.audit.sdk.routing.cache.impl;


import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.routing.cache.ICache;
import com.ge.predix.audit.sdk.routing.cache.management.AuditAsyncShutdownHandler;
import com.ge.predix.audit.sdk.routing.cache.management.AuditCacheRefresher;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AsyncClientHolderICacheImpl<T extends AuditEvent> implements ICache<String, AuditAsyncClientHolder<T>> {

    private static CustomLogger log = LoggerUtils.getLogger(AsyncClientHolderICacheImpl.class.getName());

    private final Cache<String, AuditAsyncClientHolder<T>> tenantClientCache;
    private final AuditAsyncShutdownHandler<T> shutdownHandler;
    private final ScheduledExecutorService maintenanceExecutor;

    public AsyncClientHolderICacheImpl(AuditAsyncShutdownHandler<T> shutdownHandler, AuditCacheRefresher<T> refresher,
                                       long connectionLifeTime, int numOfConnections, long refreshPeriod){
        tenantClientCache = buildCache(shutdownHandler, connectionLifeTime, numOfConnections);
        maintenanceExecutor = new ScheduledThreadPoolExecutor(1);
        this.shutdownHandler = shutdownHandler;
        startCacheRefresh(refresher, refreshPeriod);
    }

    protected Cache<String, AuditAsyncClientHolder<T>> buildCache(AuditAsyncShutdownHandler<T> shutdownHandler,
                                                               long connectionLifeTime, int numOfConnections)
    {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(connectionLifeTime, TimeUnit.MILLISECONDS)
                .maximumSize(numOfConnections)
                .removalListener(shutdownHandler::handleClientRemoval)
                .build();
    }

    protected void startCacheRefresh(AuditCacheRefresher<T> refresher, long refreshPeriod) {
        maintenanceExecutor.scheduleAtFixedRate(() -> refresher.refresh(this, refreshPeriod),
                refreshPeriod, refreshPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<AuditAsyncClientHolder<T>> get(String tenantUuid) {
        log.info("Trying to get async audit client for tenant %s", tenantUuid);
        return Optional.ofNullable(tenantClientCache.getIfPresent(tenantUuid));
    }

    @Override
    public AuditAsyncClientHolder<T> put(String tenantUuid, AuditAsyncClientHolder<T> value) {
        log.info("Inserting audit async client %s for tenant %s", value.getAuditZoneId(), tenantUuid);
        tenantClientCache.put(tenantUuid, value);
        return value;
    }

    @Override
    public void delete(String tenantUuid) {
        log.info("Invalidating async audit client for tenant %s", tenantUuid);
        tenantClientCache.invalidate(tenantUuid);
    }

    @Override
    public Map<String, AuditAsyncClientHolder<T>> getAll() {
        evict();
        return tenantClientCache.asMap();
    }

    @Override
    public void evict() {
        log.info("Running eviction");
        tenantClientCache.cleanUp();
    }

    @Override
    public void shutdown() {
        log.info("Shutting down audit client async cache");
        maintenanceExecutor.shutdownNow();
        tenantClientCache.invalidateAll();
        shutdownHandler.shutdown();
    }

    @Override
    public void gracefulShutdown() throws Exception {
            maintenanceExecutor.shutdownNow();
            shutdownHandler.gracefulShutdown(tenantClientCache.asMap().entrySet().stream()
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList()));
    }
}
