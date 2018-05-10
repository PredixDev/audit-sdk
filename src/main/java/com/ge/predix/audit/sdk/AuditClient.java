package com.ge.predix.audit.sdk;

import java.util.List;
import java.util.logging.Logger;

import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.ge.predix.eventhub.EventHubClientException;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Created by Martin Saad on 2/12/2017.
 */
public class AuditClient {
    private static Logger log = Logger.getLogger(AuditClient.class.getName());

    private final CommonClientInterface auditClientAsyncImpl;
    private final DirectMemoryMonitor directMemoryMonitor;
    @Getter(AccessLevel.PACKAGE)
    private final AuditConfiguration auditConfiguration;

    /**
     * Returns an Async audit client to publish audit messages.
     * @param configuration - auditConfiguration
     * @param callback - callback to be invoked for client's changes.
     * @throws AuditException - when the configuration is invalid.
     * @throws EventHubClientException - when fail to build eventhub client.
     */
    public AuditClient(AuditConfiguration configuration, AuditCallback callback)
            throws AuditException, EventHubClientException {
        LoggerUtils.setLogLevelFromVcap();
        ConfigurationValidator configurationValidator = ConfigurationValidatorFactory.getConfigurationValidator();
        configurationValidator.validateAuditConfiguration(configuration, AuditClientType.ASYNC);
        this.auditConfiguration = AuditConfiguration.fromConfiguration(configuration);
        directMemoryMonitor = new DirectMemoryMonitor();
        //because only prints in debug
        if(LoggerUtils.isDebugLogLevel()) {
            directMemoryMonitor.startMeasuringDirectMemory();
        }
        TracingHandler tracingHandler = TracingHandlerFactory.newTracingHandler(auditConfiguration);
        auditClientAsyncImpl = new AuditClientAsyncImpl(auditConfiguration, callback, tracingHandler );
    }

    /**
     * Logs an audit event asynchronously.
     * Result of this operation will be propagated through the AuditCallback.
     *
     * @param event - the event to log.
     * @throws AuditException - if an unexpected error occurred with auditing.
     *         IllegalStateException - if the client was shutdown.
     */
    public void audit(AuditEvent event) throws AuditException{
        auditClientAsyncImpl.audit(event);
    }

    /**
     * Logs audit events asynchronously.
     * Result of this operation will be propagated through the AuditCallback.
     * @param events - the events to log
     * @throws AuditException - if an unexpected error occurred with auditing.
     *         IllegalStateException - if the client was shutdown.
     */
    public void audit(List<AuditEvent> events) throws AuditException {
        auditClientAsyncImpl.audit(events);
    }

    /**
     * Reconnects the audit client.
     * @throws EventHubClientException - if the attempt fails.
     *         IllegalStateException - if the client was shutdown.
     */
    public void reconnect() throws EventHubClientException {
        auditClientAsyncImpl.reconnect();
    }

    /**
     * Sends a tracing message to audit service.
     * @throws EventHubClientException
     *         IllegalStateException - if the client was shutdown, or tracing is not enabled.
     */
    public void trace() throws EventHubClientException {
        if(auditConfiguration.isTraceEnabled()) {
            auditClientAsyncImpl.trace();
        }
        else{
            throw new IllegalStateException("Trace is not enabled.");
        }
    }

    /**
     * Shuts-down this client.
     * this client cannot be restarted after it was shutdown
     * @throws EventHubClientException - in case there was an error closing resources
     */
    public void shutdown() throws EventHubClientException {
        auditClientAsyncImpl.shutdown();
        directMemoryMonitor.shutdown();
    }

    /**
     * Returns the state of this client.
     */
    public AuditClientState getState(){
        return auditClientAsyncImpl.getAuditClientState();
    }

    /**
     * Sets a new authentication token for this client.
     * This API can only be invoked if the client was created with an Authentication token.
     * @param authToken - the new token
     * @throws EventHubClientException - if failed to set the token.
     * @throws AuditException - if the operation is not supported with this client configuration. i.e- the client was not
     * created with an authToken configuration.
     *         IllegalStateException - if the client was shutdown.
     */
    public void setAuthToken(String authToken) throws EventHubClientException, AuditException {
        if(auditConfiguration.getAuthenticationMethod() == AuthenticationMethod.AUTH_TOKEN) {
            auditClientAsyncImpl.setAuthToken(authToken);
            log.warning("new auth token was successfully set");
        }
        else{
            log.warning("setAuthToken operation is not supported for this client configuration");
            throw new AuditException("setAuthToken operation is not supported for this client configuration");
        }

    }

}
