package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;

/**
 * Created by 212582776 on 3/6/2018.
 */
public class TracingHandlerFactory {

    public static TracingHandler newTracingHandler(AuditConfiguration auditConfiguration, String clientType) throws AuditException {
        return auditConfiguration.isTraceEnabled()? new TracingHandlerImpl(auditConfiguration, clientType) : new TracingHandleEmptyImpl();
    }
}
