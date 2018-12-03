package com.ge.predix.audit.sdk.routing.cache.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.routing.cache.ICache;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CommonClientInterfaceICacheImpl<T extends AuditEvent> implements ICache<String, CommonClientInterface<T>> {

    private final Cache<String, CommonClientInterface<T>> tenants;

    public CommonClientInterfaceICacheImpl(long cacheRefreshPeriod, int cacheSize){
        tenants = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheRefreshPeriod, TimeUnit.MILLISECONDS)
                .maximumSize(cacheSize)
                .build();
    }

    @Override
    public Optional<CommonClientInterface<T>> get(String tenantUuid) {
        return Optional.ofNullable(tenants.getIfPresent(tenantUuid));
    }

    @Override
    public CommonClientInterface<T> put(String tenantUuid, CommonClientInterface<T> value) {
        tenants.put(tenantUuid, value);
        return value;
    }

    @Override
    public void delete(String tenantUuid) {
        tenants.invalidate(tenantUuid);
    }

    @Override
    public Map<String, CommonClientInterface<T>> getAll() {
        evict();
        return tenants.asMap();
    }

    @Override
    public void evict() {
        tenants.cleanUp();
    }

    @Override
    public void shutdown() {
        tenants.invalidateAll();
    }

    @Override
    public void gracefulShutdown() throws Exception {
        shutdown();
    }
}
