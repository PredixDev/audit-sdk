package com.ge.predix.audit.sdk;

import static com.ge.predix.audit.sdk.util.ExceptionUtils.swallowException;

import java.net.URISyntaxException;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.apache.http.client.utils.URIBuilder;

import com.ge.predix.audit.sdk.config.AbstractAuditConfiguration;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.RoutingAuditConfiguration;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.exception.AuditException;

/**
 * Created by 212582776 on 2/20/2018.
 */
public class ConfigurationValidatorImpl implements ConfigurationValidator {

    //TODO add tests
    @Override
    public void validateAuditConfiguration( final AuditConfiguration configuration) throws AuditException {
        if (configuration == null) {
            throw new AuditException("Could not initialize audit client. auditConfiguration is null.");
        }
        verifyAppName(configuration);
        verifyTracingConfig(configuration);
        verifyParamsRanges(configuration);
    }

    @Override
    public void validateConfiguration( final RoutingAuditConfiguration routingAuditConfiguration) throws AuditException {
            Set<ConstraintViolation<RoutingAuditConfiguration>> violations =
                Validation.buildDefaultValidatorFactory().getValidator().validate(routingAuditConfiguration);
        if ( !violations.isEmpty() ){
            throw new AuditException(String.format("Invalid routing configuration: {%s}", violations));
        }
        verifyAppName(routingAuditConfiguration.getTenantAuditConfig());
        validateTracingConfig(  routingAuditConfiguration.getTenantAuditConfig().getTracingInterval(),
                routingAuditConfiguration.getSharedAuditConfig().getTracingUrl(),
                routingAuditConfiguration.getTenantAuditConfig().isTraceEnabled());
    }

    private void verifyAppName(AbstractAuditConfiguration configuration) {
        if (configuration.getCfAppName() == null) {
            swallowException(()-> configuration.updateAppNameAndSpace(new VcapLoaderServiceImpl().getApplicationFromVcap()),
                    "failed to read app name and space form vcap");
        }
    }

    private void verifyParamsRanges(AuditConfiguration configuration) throws AuditException {
        //make sure all parameters are in range
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<AuditConfiguration>> violations = validator.validate(configuration);
        if (!violations.isEmpty()) {
            throw new AuditException("invalid configuration: " + violations);
        }
    }

    private void verifyTracingConfig(AuditConfiguration configuration) throws AuditException {
        //verify tracing configuration
        validateTracingConfig(configuration.getTracingInterval(), configuration.getTracingUrl(), configuration.isTraceEnabled());
    }

    private void validateTracingConfig(long tracingInterval, String tracingUrl, boolean enabled) throws AuditException {
        if ( enabled ) {
            if (tracingInterval > 0 && tracingUrl != null && !tracingUrl.isEmpty()) {
                try {
                    new URIBuilder(tracingUrl).build();
                } catch (URISyntaxException e) {
                    throw new AuditException("tracing URL is invalid: " + tracingUrl, e);
                }
            } else {
                throw new AuditException("tracing configuration is invalid");
            }
        }
    }
}
