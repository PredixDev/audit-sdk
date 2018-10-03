package com.ge.predix.audit.sdk.config;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.joda.time.DateTimeConstants;

import javax.validation.constraints.Min;

@Getter
@EqualsAndHashCode
@ToString
public class RoutingResourceConfig {

    private static final int DEFAULT_NUMBER_OF_EVENTHUB_CONNECTIONS = 100;
    private static final int MIN_NUMBER_OF_EVENTHUB_CONNECTIONS = 3;

    private static final int DEFAULT_SHARED_TENANT_CACHE_SIZE = 10000;
    private static final int MIN_SHARED_TENANT_CACHE_SIZE = 100;

    private static final long DEFAULT_CACHE_REFRESH_PERIOD = DateTimeConstants.MILLIS_PER_MINUTE * 2; //2 minutes
    private static final long MIN_CACHE_REFRESH_PERIOD = DateTimeConstants.MILLIS_PER_MINUTE * 2; //2 minutes

    private final static int DEFAULT_EVENTHUB_CONNECTION_LIFETIME = DateTimeConstants.MILLIS_PER_MINUTE * 10; //10 minutes

    private static final int DEFAULT_MAX_CONCURRENT_AUDIT_REQUESTS = 150000;
    private static final int MIN_OF_MAX_CONCURRENT_AUDIT_REQUESTS= 150000;

    private final int numOfConnections;
    private final int sharedTenantCacheSize;
    private final long cacheRefreshPeriod;
    private final long connectionLifetime;
    private final int maxConcurrentAuditRequest;


    @Builder(builderClassName = "RoutingResourceConfigBuilder")
    public RoutingResourceConfig(@Min(MIN_NUMBER_OF_EVENTHUB_CONNECTIONS) int numOfConnections,
                                 @Min(MIN_SHARED_TENANT_CACHE_SIZE) int sharedTenantCacheSize,
                                 @Min(MIN_CACHE_REFRESH_PERIOD) long cacheRefreshPeriod,
                                 @Min(MIN_CACHE_REFRESH_PERIOD) long connectionLifetime,
                                 @Min(MIN_OF_MAX_CONCURRENT_AUDIT_REQUESTS) int maxConcurrentAuditRequest) {
            this.numOfConnections = numOfConnections;
            this.sharedTenantCacheSize = sharedTenantCacheSize;
            this.cacheRefreshPeriod = cacheRefreshPeriod;
            this.connectionLifetime = connectionLifetime;
            this.maxConcurrentAuditRequest = maxConcurrentAuditRequest;
    }

    public static class RoutingResourceConfigBuilder {
        private int numOfConnections = DEFAULT_NUMBER_OF_EVENTHUB_CONNECTIONS;
        private int sharedTenantCacheSize = DEFAULT_SHARED_TENANT_CACHE_SIZE;
        private long cacheRefreshPeriod = DEFAULT_CACHE_REFRESH_PERIOD;
        private long connectionLifetime = DEFAULT_EVENTHUB_CONNECTION_LIFETIME;
        private int maxConcurrentAuditRequest = DEFAULT_MAX_CONCURRENT_AUDIT_REQUESTS;

        public RoutingResourceConfig build(){
            return new RoutingResourceConfig(this.numOfConnections,
                    this.sharedTenantCacheSize,
                    this.cacheRefreshPeriod,
                    this.connectionLifetime,
                    this.maxConcurrentAuditRequest);
        }

    }
}
