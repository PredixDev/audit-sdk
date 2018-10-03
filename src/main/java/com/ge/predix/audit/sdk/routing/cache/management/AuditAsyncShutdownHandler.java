package com.ge.predix.audit.sdk.routing.cache.management;

import com.ge.predix.audit.sdk.config.TenantAuditConfig;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.tms.AuditTokenServiceClient;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.google.common.cache.RemovalNotification;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.ge.predix.audit.sdk.util.ExceptionUtils.swallowException;

public class AuditAsyncShutdownHandler {

    private static CustomLogger log = LoggerUtils.getLogger(AuditAsyncShutdownHandler.class.getName());

    private final ExecutorService shutdownExecutor;
    private final long timeToWait;
    private final AuditTokenServiceClient tokenServiceClient;

    public AuditAsyncShutdownHandler(TenantAuditConfig config, AuditTokenServiceClient tokenServiceClient, ExecutorService shutdownExecutor) {
        this.timeToWait = generateTimeToWait(config);
        this.tokenServiceClient = tokenServiceClient;
        this.shutdownExecutor = shutdownExecutor;
    }

    public void handleClientRemoval(RemovalNotification<String, AuditAsyncClientHolder> removalEvent) {
        try {
            if (removalEvent.wasEvicted()) {
                handleEviction(removalEvent);
            }
            else {
                handleNotEviction(removalEvent);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, e, "Failed to handle audit client removal event %s , tenantUuid %s, auditZoneId %s",
                    removalEvent.getCause(), removalEvent.getKey(), removalEvent.getValue().getAuditZoneId());
        }

    }

    private void handleNotEviction(RemovalNotification<String, AuditAsyncClientHolder> notification) {
        //Explicit - removed, replaced - new audit zone id or shutdown all
        AuditAsyncClientHolder cacheObject = notification.getValue();
        log.info("Closing immediately audit client connection for audit zone id %s and tenant %s. removal cause: %s",
                cacheObject.getAuditZoneId(), cacheObject.getTenantUuid(), notification.getCause().name());
        cacheObject.getClient().shutdown();
    }

    private void handleEviction(RemovalNotification<String, AuditAsyncClientHolder> notification) {
        //Collected expired or size.
        shutdownExecutor.submit(() -> {
            AuditAsyncClientHolder cacheObject = notification.getValue();
            log.info("Shutting down audit client connection for audit zone id %s and tenant %s. removal cause: %s",
                    cacheObject.getAuditZoneId(), cacheObject.getTenantUuid(), notification.getCause().name());
            swallowException(()-> cacheObject.refreshToken(tokenServiceClient, timeToWait), String.format("Failed to renew token for tenantUuid %s and audit zone id %s while shutting down",
                    cacheObject.getTenantUuid(), cacheObject.getAuditZoneId()));
            cacheObject.getClient().gracefulShutdown();
        });
    }

    private long generateTimeToWait(TenantAuditConfig config) {
        return (config.getMaxRetryCount() +1) * config.getRetryIntervalMillis();
    }

    public void shutdown(){
        this.shutdownExecutor.shutdown();
    }

    public void gracefulShutdown(List<AuditAsyncClientHolder> holderList) throws InterruptedException {
        holderList.forEach(auditAsyncClientHolder -> shutdownExecutor.submit( () -> {
            log.info("gracefulShutdown audit zone %s of tenant %s", auditAsyncClientHolder.getAuditZoneId(), auditAsyncClientHolder.getTenantUuid());
            auditAsyncClientHolder.getClient().gracefulShutdown();
        }));
        this.shutdownExecutor.shutdown();
        log.info("Waiting to audit routing shutdown executor to finish");
        this.shutdownExecutor.awaitTermination(timeToWait, TimeUnit.MILLISECONDS);
    }

}
