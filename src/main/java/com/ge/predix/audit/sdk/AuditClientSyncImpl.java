package com.ge.predix.audit.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.VersioningException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.audit.sdk.util.StreamUtils;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.AckStatus;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.configuration.PublishConfiguration;
import com.ge.predix.eventhub.configuration.PublishSyncConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AuditClientSyncImpl extends AbstractAuditClientImpl implements CommonClientInterface {

	@Getter
	private static Logger log = Logger.getLogger(AuditClientSyncImpl.class.getName());

	public static final int PUBLISHER_TIMEOUT = 5000;

	private AuditingResultContainer result;

	public AuditClientSyncImpl(AuditConfiguration configuration, TracingHandler tracingHandler) throws EventHubClientException {
		super(configuration, tracingHandler);

		PublishConfiguration publishConfiguration = new PublishSyncConfiguration.Builder().timeout(PUBLISHER_TIMEOUT).build();
		this.client = buildClient(configuration, publishConfiguration);
		this.reconnectEngine = ReconnectStrategyFactory.getReconnectStrategy(configuration.getReconnectMode(), this::innerReconnect);
		setStateAndNotify(AuditCommonClientState.CONNECTED);
	}

	/**
	 * Audit a single event
	 *
	 * @param event - the event to be audited
	 */
	@Override
	public synchronized AuditingResult audit(AuditEvent event) throws AuditException {
		return audit(Lists.newArrayList(event));
	}

	/**
	 * Audit multiple events
	 *
	 * @param events - the events to be audited
	 */
	@Override
	public synchronized AuditingResult audit(List<AuditEvent> events) throws AuditException {
		throwIfShutdown();
		log.info(String.format("starting to audit %d messages", events.size()));
		result = new AuditingResultContainer();
		try {
			log.info(String.format("bulk size is %d", this.bulkSize));
			StreamUtils.partitionOf(events,this.bulkSize).forEach(
					auditEvents -> {
						List<AuditEvent> validEvents = validateEvents(auditEvents);
						List<AuditEvent> cachedEvents = addToEventhubCache(validEvents);
						flushWithRetries(cachedEvents.stream().collect(Collectors.toMap(AuditEvent::getMessageId,event -> event)));
					});
		} catch (NullPointerException | IllegalArgumentException e){
			//Meaning that validation service/object mapper or other object is probably null
			throw new AuditException(e.getMessage());
		}

		return result.getResult();
	}

	private List<AuditEvent> validateEvents(Collection<AuditEvent> events) {
		log.info("Trying to validate " + events.size() + " events");
		List<AuditEvent> validEvents = new ArrayList<>(events.size());
		events.forEach(event -> {
			try {
				List<ValidatorReport> report = validatorService.validate(event);
				if (!report.isEmpty()) {
					result.onFailure(event, FailReport.VALIDATION_ERROR, report.toString());
				} else {
					validEvents.add(event);
				}
			} catch (VersioningException e) {
				log.warning(e.toString());
				result.onFailure(event, FailReport.VERSION_NOT_SUPPORTED, e.toString());
			}
		});
		return validEvents;
	}

	private List<AuditEvent> addToEventhubCache(Collection<AuditEvent> events) {
		log.info("adding " + events.size() + " events to EventHub client");
		List<AuditEvent> cachedEvents = new ArrayList<>(events.size());
		events.forEach(event -> {
			try {
				String body = om.writeValueAsString(event);
				client.addMessage(event.getMessageId(), body, null);
				cachedEvents.add(event);
				log.info("event with id " + event.getMessageId() + " was added to eventhub client");
			} catch (JsonProcessingException e) {
				log.warning(e.toString());
				result.onFailure(event, FailReport.JSON_ERROR, e.toString());
			} catch (EventHubClientException.AddMessageException e) {
				log.warning(e.toString());
				result.onFailure(event, FailReport.ADD_MESSAGE_ERROR, e.toString());
			}
		});

		return cachedEvents;
	}
	
	private void flushWithRetries(Map<String, AuditEvent> auditToSend) {
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

	private void flushAndHandleAcks(Map<String, AuditEvent> auditToSend, final int retry) {
		int auditsSize = auditToSend.size();
		List<Ack> acks = flushEventhub();
		if((acks == null) || acks.isEmpty()) {
			log.info("ack list from eventhub is empty");
			auditToSend.values().forEach(audit -> {
				if (!tracingHandler.isTracingEvent(audit)){
					log.info(String.format("No Ack for message %s, send attempt number %d trying again",
							audit.getMessageId(), retry+1));
					result.onFailure(audit, FailReport.NO_ACK, NO_ACK_WAS_RECEIVED);
				} else {
					log.info(String.format("message id %s is tracing message, therefore onFailure" +
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
	
	private void notifyNoAcks(List<Ack> acks, Map<String, AuditEvent> events) {
		Set<String> eventIds = Sets.newHashSet(events.keySet());
		acks.forEach(ack -> {
			eventIds.remove(ack.getId());
		});

		eventIds.forEach(eventId -> {
			result.onFailure(events.get(eventId), FailReport.NO_ACK, "No ack");
		});
	}
	
	private boolean resendRequired(Map<String, AuditEvent> auditToSend, int retryAttempt) {
		boolean result = true;

		if (auditToSend.size() > 0 && retryAttempt != this.retryCount) {
			log.info("adding to eventhub cache all the events that need to be re-sent");
			addToEventhubCache(auditToSend.values());
		} else {
			result = false;
		}

		return result;
	}
	
	private void notifyOnFailure(Map<String, AuditEvent> auditToSend) {
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
			log.warning("flush has failed: " +e.getMessage());
			setStateAndNotify(AuditCommonClientState.DISCONNECTED);
		}
		if (acks == null){
			log.warning("received null ack list from eventhub");
		}
		return acks;
	}
	
	private void notifyAck(Ack ack, Map<String, AuditEvent> auditMap) {
		log.info(String.format("Got ack for message: %s with status: %s",ack.getId(),ack.getStatusCode().name()));
		if (ack.getStatusCode() == AckStatus.ACCEPTED) {
			handleAcceptedAcks(ack, auditMap);
		} else {
			handleNotAcceptedAcks(ack, auditMap);
		}
	}
	
	private void handleNotAcceptedAcks(Ack ack, Map<String, AuditEvent> auditMap) {
		String eventId = ack.getId();
		AuditEvent failureEvent = auditMap.get(eventId);
		if(failureEvent != null && tracingHandler.isTracingEvent(failureEvent)) {
			log.warning("The following Tracing event "+failureEvent.toString()+ " got bad ack: "+printAck(ack));
			tracingHandler.sendCheckpoint(failureEvent, LifeCycleEnum.FAIL,"bad ack: " + printAck(ack));
		} else {
			if(failureEvent != null) {
				log.info(String.format("got NACK for message id %s, nack: %s", eventId, printAck(ack)));
				result.onFailure(failureEvent, FailReport.BAD_ACK, printAck(ack));
			} else {
				log.warning(String.format("Ack for message: %s is bad, and the message is not in map.", eventId));
			}
		}
	}
	
	private void handleAcceptedAcks(Ack ack, Map<String, AuditEvent> auditMap) {
		AuditEvent successEvent = auditMap.remove(ack.getId());
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
	protected void sendTracingMessage() {

		tracingHandler.sendInitialCheckpoint().ifPresent((event) -> {
			try {
				audit(event);
			} catch (AuditException e) {
				log.info("failed to audit tracing message: " + e.getMessage());
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
