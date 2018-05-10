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

public class AuditClientSync {
	
	private static Logger log = Logger.getLogger(AuditClientSync.class.getName());
	
	private final CommonClientInterface auditClientSyncImpl;
    private final DirectMemoryMonitor directMemoryMonitor;
    @Getter(AccessLevel.PACKAGE)
    private final AuditConfiguration auditConfiguration;

    
    public AuditClientSync(AuditConfiguration configuration)
            throws AuditException, EventHubClientException {
        if (configuration == null) {
            throw new AuditException("Could not initialize audit client. auditConfiguration is null.");
        }
        LoggerUtils.setLogLevelFromVcap();
        ConfigurationValidator configurationValidator = ConfigurationValidatorFactory.getConfigurationValidator();
        configurationValidator.validateAuditConfiguration(configuration, AuditClientType.SYNC);
        auditConfiguration = AuditConfiguration.fromConfiguration(configuration);
        directMemoryMonitor = new DirectMemoryMonitor();
        //because only prints in debug
        if(LoggerUtils.isDebugLogLevel()) {
            directMemoryMonitor.startMeasuringDirectMemory();
        }
        TracingHandler tracingHandler = TracingHandlerFactory.newTracingHandler(auditConfiguration);
        auditClientSyncImpl = new AuditClientSyncImpl(auditConfiguration, tracingHandler);
    }

    /**
     * Logs an audit event synchronously.
     * @param event - the event to log.
     * @return AuditingResult -
     * @throws AuditException - if an unexpected error occurred with auditing.
     *         IllegalStateException - if the client was shutdown
     */
    public AuditingResult audit(AuditEvent event) throws AuditException{
        return auditClientSyncImpl.audit(event);
    }

    /**
     * Logs audit events synchronously.
     * @param events - the events to log.
     * @return AuditingResult -
     * @throws AuditException - if an unexpected error occurred with auditing.
     *         IllegalStateException - if the client was shutdown
     */
    public AuditingResult audit(List<AuditEvent> events) throws AuditException {
    	return auditClientSyncImpl.audit(events);
    }

    /**
     * Reconnects the audit client.
     * @throws EventHubClientException - if the attempt fails.
     *         IllegalStateException - if the client was shutdown.
     */
    public void reconnect() throws EventHubClientException {
    	auditClientSyncImpl.reconnect();
    }

    /**
     * Sends a tracing message to audit service.
     * @throws EventHubClientException
     *         IllegalStateException - if the client was shutdown, or tracing is not enabled.
     */
    public void trace() throws EventHubClientException {
        if(auditConfiguration.isTraceEnabled()) {
    	    auditClientSyncImpl.trace();
        }
        else{
            throw new IllegalStateException("Trace is not enabled in configuration.");
        }
    }

    /**
     * Shuts-down this client.
     * this client cannot be restarted after it was shutdown
     * @throws EventHubClientException - in case there was an error closing resources
     */
    public void shutdown() throws EventHubClientException {
    	auditClientSyncImpl.shutdown();
        directMemoryMonitor.shutdown();
    }

    /**
     * Returns the state of this client.
     */
    public AuditClientState getState(){
        return auditClientSyncImpl.getAuditClientState();
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
        	auditClientSyncImpl.setAuthToken(authToken);
            log.warning("new auth token was successfully set");
        } else {
            log.warning("setAuthToken operation is not supported for this client configuration");
            throw new AuditException("setAuthToken operation is not supported for this client configuration");
        }
    }
}
