package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;

/**
 * Created by 212582776 on 2/20/2018.
 */
public interface ConfigurationValidator {

    void validateAuditConfiguration(final AuditConfiguration configuration, final AuditClientType auditClientType) throws AuditException;
}
