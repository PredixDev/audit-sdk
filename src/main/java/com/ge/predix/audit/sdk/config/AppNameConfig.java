package com.ge.predix.audit.sdk.config;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;


@AllArgsConstructor
@Builder(builderClassName = "AppNameConfigBuilder")
@Getter
@EqualsAndHashCode
@ToString
public class AppNameConfig {

    public static final String APP_SCOPE_PREFIX = "stuf.app.";
    //For applicationName
    @NotBlank private final String uaaUrl;
    @NotBlank private final String clientId;
    @NotBlank private final String clientSecret;
    @NotBlank private final String appNamePrefix;

    public static class AppNameConfigBuilder {
        private String appNamePrefix = APP_SCOPE_PREFIX;
        public AppNameConfig build() {
            return new AppNameConfig(uaaUrl, clientId, clientSecret, appNamePrefix);
        }
    }

}
