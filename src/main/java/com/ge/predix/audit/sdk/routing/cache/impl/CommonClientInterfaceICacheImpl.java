package com.ge.predix.audit.sdk.routing.cache.impl;

import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.config.RoutingAuditConfiguration;
import com.ge.predix.audit.sdk.routing.cache.ICache;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
public class CommonClientInterfaceICacheImpl implements ICache<String, CommonClientInterface> {

    private final Cache<String, CommonClientInterface> tenants;

    public CommonClientInterfaceICacheImpl(long cacheRefreshPeriod, int cacheSize){
        tenants = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheRefreshPeriod, TimeUnit.MILLISECONDS)
                .maximumSize(cacheSize)
                .build();
    }

    @Override
    public Optional<CommonClientInterface> get(String tenantUuid) {
        return Optional.ofNullable(tenants.getIfPresent(tenantUuid));
    }

    @Override
    public CommonClientInterface put(String tenantUuid, CommonClientInterface value) {
        tenants.put(tenantUuid, value);
        return value;
    }

    @Override
    public void delete(String tenantUuid) {
        tenants.invalidate(tenantUuid);
    }

    @Override
    public Map<String, CommonClientInterface> getAll() {
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
