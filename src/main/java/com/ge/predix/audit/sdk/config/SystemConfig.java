package com.ge.predix.audit.sdk.config;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;



@Builder(builderClassName = "SystemConfigBuilder")
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class SystemConfig {

    @NotBlank private final String tmsUrl;
    @NotBlank private final String tokenServiceUrl;
    @NotBlank private final String clientSecret;
    @NotBlank private final String clientId;
    @NotBlank private final String canonicalServiceName;

    public static class SystemConfigBuilder {
        private String canonicalServiceName = "predix-audit";
        public SystemConfig build() {
            return new SystemConfig(this.tmsUrl, this.tokenServiceUrl, this.clientSecret, this.clientId, this.canonicalServiceName);
        }
    }


}
