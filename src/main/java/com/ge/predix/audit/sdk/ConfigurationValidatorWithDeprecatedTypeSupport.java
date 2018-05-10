package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.VcapLoadException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by 212582776 on 2/20/2018.
 */
public class ConfigurationValidatorWithDeprecatedTypeSupport implements ConfigurationValidator {

    private static Logger log = Logger.getLogger(ConfigurationValidatorWithDeprecatedTypeSupport.class.getName());

    //TODO add tests
    @Override
    public void validateAuditConfiguration( final AuditConfiguration configuration, final  AuditClientType auditClientType) throws AuditException {
        if (configuration == null) {
            throw new AuditException("Could not initialize audit client. auditConfiguration is null.");
        }
        verifyClientType(configuration, auditClientType);
        verifyAppName(configuration);
        verifyTracingConfig(configuration);
        verifyParamsRanges(configuration);


    }

    private void verifyAppName(AuditConfiguration configuration) {
        if (configuration.getAppName() == null) {
            try {
                configuration.updateAppNameAndSpace(
                        new VcapLoaderServiceImpl().getApplicationFromVcap());
            } catch (VcapLoadException e) {
                log.info("failed to read app name and space form vcap");
            }
        }
    }

    private void verifyClientType(AuditConfiguration configuration, AuditClientType auditClientType) throws AuditException {
        if(configuration.getClientType() != null && auditClientType != configuration.getClientType()){
            throw new AuditException("auditClientType is inconsistent with the created client type. This property is deprecated and should not be used");
        }
    }

    private void verifyParamsRanges(AuditConfiguration configuration) throws AuditException {
        //make sure all parameters are in range
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<AuditConfiguration>> violations = validator.validate(configuration);
        if (!violations.isEmpty()) {
            log.warning("invalid configuration: " + violations);
            throw new AuditException("invalid configuration: " + violations);
        }
    }

    private void verifyTracingConfig(AuditConfiguration configuration) throws AuditException {
        //verify tracing configuration
        if(configuration.isTraceEnabled()) {
            long tracingInterval = configuration.getTracingInterval();
            String tracingUrl = configuration.getTracingUrl();
            if (tracingInterval > 0 && tracingUrl != null && !tracingUrl.isEmpty()) {
                try {
                    URI uri  = new URI(tracingUrl);
                } catch (URISyntaxException e) {
                    log.warning("tracing URL is invalid: "+tracingUrl);
                    throw new AuditException("tracing configuration is invalid");
                }
            }
            else{
                log.warning("tracing configuration is invalid");
                throw new AuditException("tracing configuration is invalid");
            }
        }
    }
}
