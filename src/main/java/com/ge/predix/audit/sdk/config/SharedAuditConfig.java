package com.ge.predix.audit.sdk.config;

import lombok.*;

import org.hibernate.validator.constraints.NotBlank;

@AllArgsConstructor
@Builder(builderClassName = "SharedAuditConfigBuilder")
@Getter
@EqualsAndHashCode
@ToString
public class SharedAuditConfig {

    @NotBlank
    private final String uaaUrl;
    @NotBlank
    private final String uaaClientId;
    @NotBlank
    private final String uaaClientSecret;
    @NotBlank
    private final String ehubUrl;
    @NotBlank
    private final String ehubZoneId;
    @NotBlank
    private final String auditZoneId;

    //There is a case where application will turn off tracing, hence url & token are not validated here.
    private final String tracingUrl;
    private final String tracingToken;

    public static class SharedAuditConfigBuilder {

    }
}
