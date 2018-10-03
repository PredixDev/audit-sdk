package com.ge.predix.audit.sdk.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Getter
@EqualsAndHashCode
@ToString
public class TenantAuditConfig extends AbstractAuditConfiguration {

    @Min(MIN_CACHE_SIZE) private final int maxNumberOfEventsInCachePerTenant;

    @lombok.Builder(builderClassName = "TenantAuditConfigBuilder")
    public TenantAuditConfig(Boolean bulkMode,
                             long tracingInterval,
                             String auditServiceName,
                             String spaceName,
                             String cfAppName,
                             @Max(MAX_RETRY_COUNT) @Min(MIN_RETRY_COUNT) int maxRetryCount,
                             @Max(MAX_RETRY_INTERVAL_MILLIS) @Min(MIN_RETRY_INTERVAL_MILLIS) long retryIntervalMillis,
                             boolean traceEnabled,
                             @Min(MIN_CACHE_SIZE) int maxNumberOfEventsInCachePerTenant) {

        super(bulkMode, tracingInterval, auditServiceName, spaceName, maxRetryCount, retryIntervalMillis, traceEnabled, cfAppName);
        this.maxNumberOfEventsInCachePerTenant = maxNumberOfEventsInCachePerTenant;
    }

    public static class TenantAuditConfigBuilder {
        private int maxNumberOfEventsInCachePerTenant = DEFAULT_CACHE_SIZE;
        private int maxRetryCount = DEFAULT_RETRY_COUNT;
        private long retryIntervalMillis = DEFAULT_RETRY_INTERVAL_MILLIS;
        private Boolean bulkMode = true;
        private boolean traceEnabled = false;
        public TenantAuditConfig build(){
            return new TenantAuditConfig(this.bulkMode,
                    this.tracingInterval,
                    this.auditServiceName,
                    this.spaceName,
                    this.cfAppName,
                    this.maxRetryCount, retryIntervalMillis, traceEnabled, maxNumberOfEventsInCachePerTenant);
        }

    }
}
