package com.ge.predix.audit.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.ExceptionUtils;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.ge.predix.audit.sdk.util.StreamUtils;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.AckStatus;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.configuration.PublishConfiguration;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AuditClientSyncImpl<T extends AuditEvent> extends AbstractAuditClientImpl<T> {

	private static CustomLogger log = LoggerUtils.getLogger(AuditClientSyncImpl.class.getName());

	public static final int PUBLISHER_TIMEOUT = 5000;

	private AuditingResultContainer<T> result;

	public AuditClientSyncImpl(AuditConfiguration configuration, TracingHandler tracingHandler) throws EventHubClientException {
		super(configuration, tracingHandler);
		PublishConfiguration publishConfiguration = new PublishConfiguration.Builder()
				.publisherType(PublishConfiguration.PublisherType.SYNC)
				.timeout(PUBLISHER_TIMEOUT).build();
		this.client = buildClient(configuration, publishConfiguration);
		this.reconnectEngine = ReconnectStrategyFactory.getReconnectStrategy(configuration.getReconnectMode(), this::innerReconnect, logPrefix);
		setStateAndNotify(AuditCommonClientState.CONNECTED);
	}

	/**
	 * Audit a single event
	 *
	 * @param event - the event to be audited
	 */
	@Override
	public synchronized AuditingResult<T> audit(T event) throws AuditException {
		return audit(Collections.singletonList(event));
	}

	/**
	 * Audit multiple events
	 *
	 * @param events - the events to be audited
	 */
	@Override
	public synchronized AuditingResult<T> audit(List<T> events) throws AuditException {
		throwIfShutdown();
		log.info("starting to audit %d messages", events.size());
		result = new AuditingResultContainer<T>();
		log.info("bulk size is %d", this.bulkSize);
		StreamUtils.partitionOf(events, this.bulkSize).forEach(
				auditEvents -> {
					List<T> validEvents = validateEvents(auditEvents);
					List<T> cachedEvents = addToEventhubCache(validEvents);
					flushWithRetries(cachedEvents.stream().collect(Collectors.toMap(AuditEvent::getMessageId, event -> event)));
				});
		return result.getResult();
	}

	private List<T> validateEvents(Collection<T> events) {
		log.logWithPrefix(Level.INFO, logPrefix, "Trying to validate %d events", events.size());
		List<T> validEvents = new ArrayList<>(events.size());

		events.forEach(event -> {
				List<ValidatorReport> report = ValidatorServiceImpl.instance.validate(event);
				if (!report.isEmpty()) {
					result.onFailure(event, FailCode.VALIDATION_ERROR, report.toString());
				} else {
					validEvents.add(event);
				}
		});
		return validEvents;
	}

	private List<T> addToEventhubCache(Collection<T> events) {
		log.info("adding %d events to EventHub client", events.size());
		List<T> cachedEvents = new ArrayList<>(events.size());
		events.forEach(event -> {
			try {
				String body = om.writeValueAsString(event);
				client.addMessage(event.getMessageId(), body, null);
				cachedEvents.add(event);
				log.info("event with id %s was added to eventhub client", event.getMessageId());
			} catch (JsonProcessingException e) {
				log.warning(e.toString());
				result.onFailure(event, FailCode.JSON_ERROR, "add event to cache - json processing error", e);
			} catch (EventHubClientException e) {
				log.warning(e.toString());
				result.onFailure(event, FailCode.ADD_MESSAGE_ERROR, "add event to cache - event hub client error", e);
			}
		});

		return cachedEvents;
	}
	
	private void flushWithRetries(Map<String, T> auditToSend) {
		if(!auditToSend.isEmpty()) {
			for (int i = 0; i <= this.retryCount; i++) {
				if(isSendMode()) {
					flushAndHandleAcks(auditToSend, i);
					if (!resendRequired(auditToSend, i)) {
						break;
					}
				}
				
				try {
					Thread.sleep(noAckLimit);
				} catch (InterruptedException e) {
					log.warning("sync publishing was interrupted while performing retry");
				}

			}
			notifyOnFailure(auditToSend);
		} else {
			log.info("flushWithRetries: auditToSend is empty.");
		}
	}

	private void flushAndHandleAcks(Map<String, T> auditToSend, final int retry) {
		int auditsSize = auditToSend.size();
		List<Ack> acks = flushEventhub();
		if((acks == null) || acks.isEmpty()) {
			log.info("ack list from eventhub is empty");
			auditToSend.values().forEach(audit -> {
				if (!tracingHandler.isTracingEvent(audit)){
					log.info(String.format("No Ack for message %s, send attempt number %d trying again",
							audit.getMessageId(), retry+1));
					result.onFailure(audit, FailCode.NO_ACK, NO_ACK_WAS_RECEIVED);
				} else {
					log.info(String.format("message id %s is tracing message, therefore onClientError" +
							" callback is not called.", audit.toString()));
				}
			});
		} else {
			setStateAndNotify(AuditCommonClientState.ACKED);
			acks.forEach(ack -> notifyAck(ack, auditToSend));
			if (auditsSize > acks.size()) {
				notifyNoAcks(acks, auditToSend);
			}
		}
	}
	
	private void notifyNoAcks(List<Ack> acks, Map<String, T> events) {
		Set<String> eventIds = Sets.newHashSet(events.keySet());
		acks.forEach(ack -> eventIds.remove(ack.getId()));

		eventIds.forEach(eventId -> {
			result.onFailure(events.get(eventId), FailCode.NO_ACK, "No ack");
		});
	}
	
	private boolean resendRequired(Map<String, T> auditToSend, int retryAttempt) {
		boolean result = true;

		if (auditToSend.size() > 0 && retryAttempt != this.retryCount) {
			log.info("adding to eventhub cache all the events that need to be re-sent");
			addToEventhubCache(auditToSend.values());
		} else {
			result = false;
		}

		return result;
	}
	
	private void notifyOnFailure(Map<String, T> auditToSend) {
		log.info(String.format("Finish to flush, amount of remaining audits that didn't succeed: %d",
				auditToSend.size()));

		auditToSend.values().forEach(event -> {
			if (!tracingHandler.isTracingEvent(event)){
				log.info(String.format("Tried to send the event %d times, and didn't get any ACCEPTED ack. Stopping retrying.", retryCount + 1));
			}
			else{
				tracingHandler.sendCheckpoint(event,LifeCycleEnum.FAIL, "No Ack");
			}
		});
	}
	
	private List<Ack> flushEventhub() {
		List<Ack> acks = null;
		try {
			acks = this.client.flush();
		} catch (EventHubClientException e) {
			log.warning("flush has failed: " + ExceptionUtils.toString(e));
			setStateAndNotify(AuditCommonClientState.DISCONNECTED);
		}
		if (acks == null){
			log.warning("received null ack list from eventhub");
		}
		return acks;
	}
	
	private void notifyAck(Ack ack, Map<String, T> auditMap) {
		log.info(String.format("Got ack for message: %s with status: %s",ack.getId(),ack.getStatusCode().name()));
		if (ack.getStatusCode() == AckStatus.ACCEPTED) {
			handleAcceptedAcks(ack, auditMap);
		} else {
			handleNotAcceptedAcks(ack, auditMap);
		}
	}
	
	private void handleNotAcceptedAcks(Ack ack, Map<String, T> auditMap) {
		String eventId = ack.getId();
		T failureEvent = auditMap.get(eventId);
		if(failureEvent != null && tracingHandler.isTracingEvent(failureEvent)) {
			log.warning("The following Tracing event "+failureEvent.toString()+ " got bad ack: "+printAck(ack));
			tracingHandler.sendCheckpoint(failureEvent, LifeCycleEnum.FAIL,"bad ack: " + printAck(ack));
		} else {
			if(failureEvent != null) {
				log.info(String.format("got NACK for message id %s, nack: %s", eventId, printAck(ack)));
				result.onFailure(failureEvent, FailCode.BAD_ACK, printAck(ack));
			} else {
				log.warning(String.format("Ack for message: %s is bad, and the message is not in map.", eventId));
			}
		}
	}
	
	private void handleAcceptedAcks(Ack ack, Map<String, T> auditMap) {
		T successEvent = auditMap.remove(ack.getId());
		if(!tracingHandler.isTracingAck(ack)) {
			if(null != successEvent) {
				result.onSuccess(successEvent);
			} else {
				log.warning(String.format("Ack for message: %s is success but message is not in the map",ack.getId()));
			}
		}
		else{
			tracingHandler.sendCheckpoint(ack);
		}
	}

	@Override
	protected void sendTracingMessage(){
		tracingHandler.sendInitialCheckpoint().ifPresent(auditTracingEvent -> {
			try {
				audit((T)auditTracingEvent);
			} catch (AuditException e) {
				log.info("failed to audit tracing message: " + ExceptionUtils.toString(e));
			}
		});
	}
	

	private void innerReconnect() {
		try {
			reconnect();
		} catch (EventHubClientException e) {
			setStateAndNotify(AuditCommonClientState.DISCONNECTED);
		}
	}
}
