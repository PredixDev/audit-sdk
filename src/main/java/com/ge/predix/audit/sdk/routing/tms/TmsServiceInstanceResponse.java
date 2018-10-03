package com.ge.predix.audit.sdk.routing.tms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class TmsServiceInstanceResponse<T> {
    private final TmsServiceInstance<T> tmsServiceInstance;
    private final int statusCode;
    private final String response;
}
