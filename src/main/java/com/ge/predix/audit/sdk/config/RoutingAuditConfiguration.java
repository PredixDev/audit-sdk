package com.ge.predix.audit.sdk.config;


import com.ge.predix.audit.sdk.ConfigurationValidatorFactory;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.RoutingAuditException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author Kobi (212584872) on 1/10/2017.
 * @author Igor (212579997)
 */
//TODO add documentation for limitation
@Getter
@Builder(builderClassName = "RoutingAuditConfigurationBuilder")
@EqualsAndHashCode
@ToString
public class RoutingAuditConfiguration {

    @NotNull
    @Valid
    private final SystemConfig systemConfig;
    @NotNull
    @Valid
    private final AppNameConfig appNameConfig;
    @NotNull
    @Valid
    private final SharedAuditConfig sharedAuditConfig;
    @NotNull
    @Valid
    private final TenantAuditConfig tenantAuditConfig;
    @NotNull
    @Valid
    private final RoutingResourceConfig routingResourceConfig;

    RoutingAuditConfiguration(@NotNull @Valid SystemConfig systemConfig,
                              @NotNull @Valid AppNameConfig appNameConfig,
                              @NotNull @Valid SharedAuditConfig sharedAuditConfig,
                              @NotNull @Valid TenantAuditConfig tenantAuditConfig,
                              @NotNull @Valid RoutingResourceConfig routingResourceConfig) throws RoutingAuditException {
        this.systemConfig = systemConfig;
        this.appNameConfig = appNameConfig;
        this.sharedAuditConfig = sharedAuditConfig;
        this.tenantAuditConfig = tenantAuditConfig;
        this.routingResourceConfig = routingResourceConfig;
    }

    public static class RoutingAuditConfigurationBuilder {
        private RoutingResourceConfig routingResourceConfig;
        private TenantAuditConfig tenantAuditConfig;

        public RoutingAuditConfiguration build() throws RoutingAuditException {
            this.routingResourceConfig = (this.routingResourceConfig == null) ? RoutingResourceConfig.builder().build() : this.routingResourceConfig;
            this.tenantAuditConfig = (this.tenantAuditConfig == null) ? TenantAuditConfig.builder().build() : this.tenantAuditConfig;
            return new RoutingAuditConfiguration(this.systemConfig, this.appNameConfig, this.sharedAuditConfig, this.tenantAuditConfig, this.routingResourceConfig)
                    .validate();
        }
    }

    private RoutingAuditConfiguration validate() {
        try {
            ConfigurationValidatorFactory.getConfigurationValidator().validateConfiguration(this);
            return this;
        } catch (AuditException e) {
            throw new RoutingAuditException(e);
        }
    }
}