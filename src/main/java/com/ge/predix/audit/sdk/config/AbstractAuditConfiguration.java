package com.ge.predix.audit.sdk.config;

import com.ge.predix.audit.sdk.config.vcap.VcapApplication;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public abstract class AbstractAuditConfiguration {
    public static final int MAX_RETRY_COUNT = 5;
    public static final int DEFAULT_RETRY_COUNT = 2;
    public static final int MIN_RETRY_COUNT = 0;
    public static final long MAX_RETRY_INTERVAL_MILLIS = 20000;

    public static final long DEFAULT_RETRY_INTERVAL_MILLIS = 10000;
    public static final long MIN_RETRY_INTERVAL_MILLIS = 2000;
    public static final int DEFAULT_CACHE_SIZE = 50000;
    public static final int MIN_CACHE_SIZE = 1000;

    //Export to abstract configuration
    private Boolean bulkMode;

    private long tracingInterval;
    private String auditServiceName;
    private String spaceName;
    @Max(MAX_RETRY_COUNT)
    @Min(MIN_RETRY_COUNT)
    private int maxRetryCount;
    @Max(MAX_RETRY_INTERVAL_MILLIS)
    @Min(MIN_RETRY_INTERVAL_MILLIS)
    private long retryIntervalMillis;

    private boolean traceEnabled;
    private String cfAppName;


    public void updateAppNameAndSpace(VcapApplication vcapApplication) {
        if (null != vcapApplication) {
            cfAppName = vcapApplication.getAppName();
            spaceName = vcapApplication.getSpaceName();
        }
    }

}
