package com.ge.predix.audit.sdk.config.vcap;

/**
 * Created by 212584872 on 1/8/2017.
 */
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditServiceCredentials {
    @SerializedName("audit-pub-client-scope")
    private List<String> auditPubClientScope;
    @SerializedName("audit-query-api-scope")
    private String auditQueryApiScope;
    @SerializedName("audit-query-api-url")
    private String auditQueryApiUrl;
    @SerializedName("event-hub-uri")
    private String eventHubUri;
    @SerializedName("event-hub-zone-id")
    private String eventHubZoneId;
    @SerializedName("tracing-url")
    private String tracingUrl;
    @SerializedName("tracing-token")
    private String tracingToken;
    @SerializedName("tracing-interval")
    private long tracingInterval;
}