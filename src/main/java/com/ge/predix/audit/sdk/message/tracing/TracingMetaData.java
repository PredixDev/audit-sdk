package com.ge.predix.audit.sdk.message.tracing;

import com.ge.predix.audit.sdk.AuthenticationMethod;
import com.ge.predix.audit.sdk.config.ReconnectMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * Created by Martin Saad on 4/27/2017.
 */
@AllArgsConstructor
@Builder
@ToString
@Data
public class TracingMetaData implements Serializable{
    private String customData;
    private String eventhubHost;
    private String uaaUrl;
    private boolean bulkMode;
    private String auditServiceName;
    private String spaceName;
    private String appName;
    private int retryCount;
    private long retryInterval;
    private int cacheSize;
    private ReconnectMode reconnectMode;
    private AuthenticationMethod authenticationMethod;
    private String clientType;
}
