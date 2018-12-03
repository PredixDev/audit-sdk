package com.ge.predix.audit.sdk.routing.cache.management;

import com.ge.predix.audit.sdk.config.vcap.AuditServiceCredentials;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.routing.cache.ICache;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.tms.AuditTmsClient;
import com.ge.predix.audit.sdk.routing.tms.AuditTokenServiceClient;
import com.ge.predix.audit.sdk.routing.tms.TmsServiceInstance;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AllArgsConstructor;

import java.util.Map.Entry;

import static com.ge.predix.audit.sdk.util.ExceptionUtils.swallowException;

//This class will refresh the auditCache
@AllArgsConstructor
public class AuditCacheRefresher<T extends AuditEvent> {

    private static CustomLogger log = LoggerUtils.getLogger(AuditCacheRefresher.class.getName());

    private final AuditTmsClient tms;
    private final AuditTokenServiceClient tokenService;

    public void refresh(ICache<String, AuditAsyncClientHolder<T>> auditICacheToRefresh, long refreshPeriod) {
        auditICacheToRefresh.getAll().entrySet() //Refresh each record
                .forEach(e -> swallowException(()-> refreshTokenForAuditClient(e, auditICacheToRefresh, refreshPeriod),
                        String.format("Audit cache refresh executor could not refresh tenant %s", e.getKey())));
        auditICacheToRefresh.evict(); //Evict expired records
    }

    private void refreshTokenForAuditClient(Entry<String, AuditAsyncClientHolder<T>> entry,
                                            ICache<String, AuditAsyncClientHolder<T>> auditICacheToRefresh,
                                            long refreshPeriod) {
            String tenantUuid = entry.getKey();
            AuditAsyncClientHolder<T> auditAsyncClientHolder = entry.getValue();
            tms.fetchServiceInstance(tenantUuid) //try to get new instance
                    .filter(instance -> isAuditZoneIdExistsInTms(auditAsyncClientHolder, instance)) //check if the received instance is equal to the instance that we already have
                    .map(instance -> refreshTokenForAuditClient(tenantUuid, auditAsyncClientHolder, refreshPeriod)) //try to refresh token
                    .orElseGet( () -> removeCacheObject(tenantUuid, auditAsyncClientHolder, auditICacheToRefresh));
    }

    private boolean isAuditZoneIdExistsInTms(AuditAsyncClientHolder<T> clientCacheObject, TmsServiceInstance<AuditServiceCredentials> instance) {
        return clientCacheObject.getAuditZoneId().equals(instance.getServiceInstanceUuid());
    }

    private AuditAsyncClientHolder<T> refreshTokenForAuditClient(String tenantUuid, AuditAsyncClientHolder<T> auditAsyncClientHolder, long refreshPeriod) {
         swallowException(()-> auditAsyncClientHolder.refreshToken(tokenService, refreshPeriod),
                String.format("Failed to refresh token for tenant  %s and audit zone id %s", tenantUuid, auditAsyncClientHolder.getAuditZoneId()));
         return auditAsyncClientHolder;
    }

    private AuditAsyncClientHolder<T> removeCacheObject(String tenantUuid,
                                                     AuditAsyncClientHolder<T> auditAsyncClientHolder,
                                                     ICache<String, AuditAsyncClientHolder<T>> auditICacheToRefresh) {
        log.info("Shutting down audit instance of tenant %s since there is no audit instance for this tenant or there is different audit instance", tenantUuid);
        auditICacheToRefresh.delete(tenantUuid);
        return auditAsyncClientHolder;
    }


}
