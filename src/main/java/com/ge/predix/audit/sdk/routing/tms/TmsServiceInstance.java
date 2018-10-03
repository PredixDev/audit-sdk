package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsServiceInstance<T> {
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("serviceInstanceName")
    private String serviceInstanceName;
    @JsonProperty("serviceInstanceUuid")
    private String serviceInstanceUuid;
    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("canonicalServiceName")
    private String canonicalServiceName;
    @JsonProperty("status")
    private String status;
    @JsonProperty("createdBy")
    private String createdBy;
    @JsonProperty("createdOn")
    private long createdOn;
    @JsonProperty("updatedOn")
    private long updatedOn;
    @JsonProperty("credentials")
    private T credentials;
    @JsonProperty("managed")
    private boolean managed;
}

