package com.ge.predix.audit.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.validator.ValidatorService;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.client.Client;
import com.ge.predix.eventhub.configuration.EventHubConfiguration;
import com.ge.predix.eventhub.configuration.PublishConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public abstract class AbstractAuditClientImpl implements CommonClientInterface {

	public static final String NO_ACK_WAS_RECEIVED = "No ACK was received";
	@Getter
	private static Logger log = Logger.getLogger(AbstractAuditClientImpl.class.getName());

	protected final AuditConfiguration configuration;

	@Getter @Setter(AccessLevel.PACKAGE)
	protected int bulkSize = 150;

	@Setter(AccessLevel.PACKAGE)
	protected int retryCount;

	@Setter(AccessLevel.PACKAGE)
	protected long noAckLimit;

	protected final boolean automaticTokenRenew;

	@Setter(AccessLevel.PACKAGE)
	protected ValidatorService validatorService;

	@Setter(AccessLevel.PACKAGE)
	protected ObjectMapper om;

	@Setter(AccessLevel.PACKAGE)
	protected Client client;

	protected ScheduledExecutorService tracingExecutor;

	protected TracingHandler tracingHandler;

	@Setter(AccessLevel.PACKAGE)
	@Getter(AccessLevel.PACKAGE)
	protected ReconnectStrategy reconnectEngine;

	@Getter
	protected volatile AuditCommonClientState auditCommonClientState = AuditCommonClientState.NOT_INIT;

	protected boolean isSendMode() {
		return auditCommonClientState.equals(AuditCommonClientState.CONNECTED) || auditCommonClientState.equals(AuditCommonClientState.ACKED);
	}

	protected abstract void sendTracingMessage();

	public AbstractAuditClientImpl(AuditConfiguration configuration, TracingHandler tracingHandler) {
		this.configuration = configuration;
		this.tracingHandler = tracingHandler;
		this.om = new ObjectMapper();
		this.retryCount = configuration.getMaxRetryCount();
		this.noAckLimit = configuration.getRetryIntervalMillis();
		this.validatorService = new ValidatorServiceImpl();
		this.automaticTokenRenew = (configuration.getAuthenticationMethod() != AuthenticationMethod.AUTH_TOKEN );

		if(configuration.isTraceEnabled()){
			tracingExecutor = Executors.newScheduledThreadPool(1);
			startTracingRepetitive();
		}
	}

	/**
	 * Reconnect audit client manually
	 * @throws EventHubClientException in case of EventHub failure
	 */
	@Override
	public synchronized void reconnect() throws EventHubClientException {
		throwIfShutdown();
		setStateAndNotify(AuditCommonClientState.CONNECTING);
		log.warning("reconnecting audit client");
		client.reconnect();
		setStateAndNotify(AuditCommonClientState.CONNECTED);
		log.warning("audit client is up and running");
	}


	/**
	 * Shuts down the audit client
	 */
	@Override
	public synchronized void shutdown() {
		setStateAndNotify(AuditCommonClientState.SHUTDOWN);
		Optional.ofNullable(tracingExecutor).ifPresent(ExecutorService::shutdownNow);
		client.shutdown();
	}

	/**
	 * Sets new authentication token to the eventhub client
	 */
	@Override
	public void setAuthToken(String authToken) throws AuditException, EventHubClientException {
		throwIfShutdown();
		client.setAuthToken(authToken);
        setStateAndNotify(auditCommonClientState.CONNECTED);
	}

	/**
	 * Sends a tracing event
	 */
	@Override
	public void trace() {
		throwIfShutdown();
		sendTracingMessage();
	}

	@Override
	public AuditClientState getAuditClientState(){
		switch(auditCommonClientState){
			case NOT_INIT:
			case DISCONNECTED:
				return AuditClientState.DISCONNECTED;
			case CONNECTING:
				return AuditClientState.CONNECTING;
			case CONNECTED:
			case ACKED:
				return AuditClientState.CONNECTED;
			case SHUTDOWN:
				return AuditClientState.SHUTDOWN;
			default:
				return AuditClientState.DISCONNECTED;
		}
	}

	protected void throwIfShutdown() {
		if(auditCommonClientState == AuditCommonClientState.SHUTDOWN){
			throw new IllegalStateException("Illegal operation - client was shutdown.");
		}
	}

	protected Client buildClient(AuditConfiguration auditConfiguration, PublishConfiguration publishConfiguration)
			throws EventHubClientException {
		auditCommonClientState = AuditCommonClientState.CONNECTING;
		log.warning("building EventHub client with audit configuration: " + auditConfiguration);
		EventHubConfiguration configuration = new EventHubConfiguration.Builder()
				.host(auditConfiguration.getEhubHost())
				.port(auditConfiguration.getEhubPort())
				.authURL(auditConfiguration.getUaaUrl())
				.zoneID(auditConfiguration.getEhubZoneId())
				.clientID(auditConfiguration.getUaaClientId())
				.clientSecret(auditConfiguration.getUaaClientSecret())
				.automaticTokenRenew(automaticTokenRenew)
				.publishConfiguration(publishConfiguration)
				.build();

		Client client = new Client(configuration);
		if(auditConfiguration.getAuthenticationMethod() == AuthenticationMethod.AUTH_TOKEN){
			client.setAuthToken(auditConfiguration.getAuthToken());
		}
		return client;
	}

	protected void setStateAndNotify(AuditCommonClientState state) {
		if(auditCommonClientState == AuditCommonClientState.SHUTDOWN){
			log.warning("client is already shutdown, state was not set.");
			return;
		}
		auditCommonClientState = state;
		reconnectEngine.notifyStateChanged(state);
	}

	private void startTracingRepetitive(){
		tracingExecutor.scheduleAtFixedRate(this::sendTracingMessage, configuration.getTracingInterval(),
				configuration.getTracingInterval(), TimeUnit.MILLISECONDS);

	}

	public static String printAck(final Ack ack) {
		if(ack != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("ack: {");
			sb.append("message id: ");
			sb.append(ack.getId());
			sb.append(", status: ");
			sb.append(ack.getStatusCode());
			sb.append(", description: ");
			sb.append(ack.getDesc());
			sb.append(", partition: ");
			sb.append(ack.getPartition());
			sb.append(", zone id: ");
			sb.append(ack.getZoneId());
			sb.append("}");
			return  sb.toString();
		}
		return null;
	}
}
