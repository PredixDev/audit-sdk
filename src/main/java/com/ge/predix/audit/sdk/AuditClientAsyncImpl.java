package com.ge.predix.audit.sdk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
import com.ge.predix.eventhub.client.Client;
import com.ge.predix.eventhub.configuration.PublishConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import lombok.Getter;
import lombok.Setter;


/**
 * Created by Martin Saad on 1/31/2017.
 */
public class AuditClientAsyncImpl<T extends AuditEvent> extends AbstractAuditClientImpl<T> {

	static final String STREAM_CLOSED = "stream closed";
	private static final String AUDIT_CACHE_IS_FULL = "audit cache is full";
	private static final String UNAUTHENTICATED = "UNAUTHENTICATED";

	private static CustomLogger log = LoggerUtils.getLogger(AuditClientAsyncImpl.class.getName());

	@Getter
	protected final AuditCallback<T> callback;

	@Setter
	private int queueSize;

	@Getter
	private Map<String, EventContainer<T>> eventMap;

	@Getter
	private LinkedBlockingQueue<QueueElement> eventQueue;
	
	@Getter
	private ScheduledExecutorService retryExecutorService;

	public AuditClientAsyncImpl(AuditConfiguration configuration, AuditCallback<T> callback, TracingHandler tracingHandler)
			throws AuditException {
		super(configuration, tracingHandler);
		log.logWithPrefix(Level.WARNING, logPrefix, "initializing auditClientAsync");

		PublishConfiguration publishConfiguration = new PublishConfiguration.Builder()
				.publisherType(PublishConfiguration.PublisherType.ASYNC)
				.ackType(PublishConfiguration.AcknowledgementOptions.ACKS_AND_NACKS).build();
		try {
			this.client = buildClient(configuration, publishConfiguration);

			this.reconnectEngine = ReconnectStrategyFactory.getReconnectStrategy(configuration.getReconnectMode(), this::innerReconnect, logPrefix);
			this.callback = callback;
			this.eventMap = Maps.newConcurrentMap();
			this.queueSize = configuration.getMaxNumberOfEventsInCache();
			this.eventQueue = Queues.newLinkedBlockingQueue(queueSize);

			retryExecutorService = Executors.newScheduledThreadPool(1);
			retryExecutorService.scheduleAtFixedRate(
					this::handleNotAcceptedEvents, noAckLimit, noAckLimit, TimeUnit.MILLISECONDS);
			handleEventHubCallback();
		} catch (EventHubClientException e) {
			throw new AuditException("Event hub client exception", e);
		}
		setStateAndNotify(AuditCommonClientState.CONNECTED);
	}

	/**
	 * Audit asynchronously a single event.
	 * @param event - the event to be logged.
	 * @throws AuditException when there was an internal error that could not be handled by the client or illegal states like client was shutdown.
	 */
	@Override
	public AuditingResult<T> audit(T event) throws AuditException {
		if (event != null) {
			audit(Collections.singletonList(event));
		}
		return AuditingResult.emptyResults();
	}

	/**
	 * Audit asynchronously multiple events.
	 *
	 * @param events the events to be logged.
	 * @throws AuditException when there was an internal error that could not be handled by the client or illegal states like client was shutdown.
	 */
	@Override
	public synchronized AuditingResult<T> audit(List<T> events) throws AuditException {
		try {
			throwIfShutdown();
			if (events != null) {
				log.logWithPrefix(Level.INFO, logPrefix, "starting audit %d messages", events.size());
				StreamUtils.partitionOf(events, this.bulkSize).forEach(
						auditEvents -> {
							List<T> validEvents = validateEvents(auditEvents);
							addEventsToCache(validEvents);
							addToEventhubCacheAndFlush(validEvents);
						});
			}
			return AuditingResult.emptyResults();
		} catch (Exception e) {
			throw new AuditException("An error occurred while auditing", e);
		}
	}

	/******************************************** PRIVATE ***********************************/

	private List<T> validateEvents(Collection<T> events) {
		log.logWithPrefix(Level.INFO, logPrefix, "Trying to validate %d events", events.size());
		List<T> validEvents = new ArrayList<>(events.size());
		List<AuditEventFailReport<T>> failReports = new ArrayList<>();

		events.forEach(event -> {
				List<ValidatorReport> report = ValidatorServiceImpl.instance.validate(event);
				if (!report.isEmpty()) {
					failReports.add(AuditEventFailReport.<T>builder()
							.auditEvent(event)
							.failureReason(FailCode.VALIDATION_ERROR)
							.description(report.toString())
							.build());
				} else {
					validEvents.add(event);
				}
		});

		if(!failReports.isEmpty()) {
			callback.onFailure(AuditAsyncResult.<T>builder().failReports(failReports).build());
		}
		return validEvents;
	}



	private void addToEventhubCache(Collection<T> events) {
		log.logWithPrefix(Level.INFO, logPrefix, "adding %d events to EventHub client", events.size());
		events.forEach(event -> {
			try {
				String body = om.writeValueAsString(event);
				client.addMessage(event.getMessageId(), body, null);
			} catch (JsonProcessingException e) {
				log.logWithPrefix(Level.INFO, e, logPrefix, "failed to parse event.");
				updateEventFailureDetails(event.getMessageId(), FailCode.JSON_ERROR, "failed to parse event", e);
			} catch (EventHubClientException e) {
				log.logWithPrefix(Level.INFO, e, logPrefix, "failed to add the event to eventhub cache");
				updateEventFailureDetails(event.getMessageId(), FailCode.ADD_MESSAGE_ERROR, "failed to add the event to eventhub cache", e);
			}
		});
	}

	void innerReconnect(){
		try {
			reconnect();
		} catch (EventHubClientException e) {
			log.logWithPrefix(Level.WARNING, e, logPrefix, "reconnect failed in reconnectStrategy");
			setStateAndNotify(AuditCommonClientState.DISCONNECTED);
			callback.onClientError(ClientErrorCode.RECONNECT_FAILURE, ExceptionUtils.toString(e));
		}
	}

	void addToEventhubCacheAndFlush(Collection<T> auditToSend){
		if(isSendMode()) {
			if (auditToSend.size() > 0) {
				addToEventhubCache(auditToSend);
				try {
					client.flush();
					log.logWithPrefix(Level.INFO, logPrefix, "%d messages sent", auditToSend.size());
				} catch (EventHubClientException e) {
					log.logWithPrefix(Level.WARNING, e, logPrefix, "failed to publish audit events");
				}
			}
		}
		else{
			log.logWithPrefix(Level.WARNING, logPrefix, "flush is not attempted while client is disconnected");
		}
	}

	EventContainer<T> removeElementFromCache(String messageId){
		EventContainer<T> eventContainer = eventMap.remove(messageId);
		eventQueue.remove(QueueElement.builder().messageId(messageId).build());
		return eventContainer;
	}

	Optional<AuditEventFailReport<T>> removeLatestElementFromCache(){
		log.logWithPrefix(Level.INFO, logPrefix, "queue size reached to limit of %d elements. removing the oldest event", queueSize);
		QueueElement polledElement = eventQueue.poll();
		if(polledElement == null){
			return Optional.empty();
		}
		
		String messageId = polledElement.getMessageId();
		EventContainer<T> eventContainer = eventMap.remove(messageId);
		if(eventContainer == null){
			return Optional.empty();
		}
		
		AuditEventFailReport<T> auditEventFailReport = AuditEventFailReport.<T>builder()
					.auditEvent(eventContainer.getAuditEvent())
					.failureReason(FailCode.CACHE_IS_FULL)
					.description(generateLogs(logPrefix, AUDIT_CACHE_IS_FULL))
					.build();
		
		return Optional.of(auditEventFailReport);
	}

	void addEventsToCache(Collection<T> events) {
		List<AuditEventFailReport<T>> failReports = new ArrayList<>();
		//if queue had reached its limit - remove the oldest event and callback as failure
		events.forEach(event -> {
			removeEventsWhenLimitReached().ifPresent(failReports::add);
			addEventToCache(event).ifPresent(failReports::add);
		});
		if(!failReports.isEmpty()) {
			callback.onFailure(AuditAsyncResult.<T>builder().failReports(failReports).build());
		}
	}

	private Optional<AuditEventFailReport<T>> removeEventsWhenLimitReached() {
		if (eventQueue.size() >= queueSize){
			return removeLatestElementFromCache();
		}
		return Optional.empty();
	}

	private Optional<AuditEventFailReport<T>> addEventToCache(T event){
		String messageId = event.getMessageId();

		if(eventMap.containsKey(messageId)) {
			return Optional.of(AuditEventFailReport.<T>builder()
					.auditEvent(event)
					.failureReason(FailCode.DUPLICATE_EVENT)
					.description(String.format("Event with id %s was already added to the cache in the past", messageId))
					.build());
		}

		@SuppressWarnings("unchecked") T cloned = (T) event.clone();
		eventMap.put(messageId,new EventContainer<>(cloned));
		eventQueue.offer(QueueElement.builder().messageId(messageId).build());

		log.logWithPrefix(Level.INFO, logPrefix, "event with id %s was added successfully to cache eventMap" +
						" size: %d, eventQueue size: %d",
				event.getMessageId(), eventMap.size(), eventQueue.size());

		return Optional.empty();
	}

	Client.PublishCallback handleEventHubCallback() throws EventHubClientException {
		log.logWithPrefix(Level.WARNING, logPrefix,"registering eventhub callback");
		Client.PublishCallback publisherCallback = new Client.PublishCallback() {

			@Override
			public void onAck(List<Ack> list) {
				log.logWithPrefix(Level.INFO, logPrefix,"new callback onAck from eventHub");
				List<T> successList = new ArrayList<>();
				if ((list != null) && !list.isEmpty()){
					setStateAndNotify(AuditCommonClientState.ACKED);
					list.forEach(ack -> handleAck(ack).ifPresent(e -> {
                        successList.add(e);
                        log.logWithPrefix(Level.INFO, logPrefix,"ack from message id: %s was successful."
                                , e.getMessageId());
                    }));
				} else{
					log.logWithPrefix(Level.INFO, logPrefix,"ack list is empty");
				}

				if (!successList.isEmpty()) {
					log.logWithPrefix(Level.INFO, logPrefix,"Notifying the user on success.");
					callback.onSuccess(successList);
				}
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.logWithPrefix(Level.INFO, throwable, logPrefix, "new callback onClientError from eventHub");
				if (throwable.getMessage() != null && throwable.getMessage().contains(STREAM_CLOSED)) {
					setStateAndNotify(AuditCommonClientState.DISCONNECTED);
					getCallback().onClientError(ClientErrorCode.STREAM_IS_CLOSE, ExceptionUtils.toString(throwable));
				}
				else if(throwable.getMessage() != null && throwable.getMessage().contains(UNAUTHENTICATED)){
					getCallback().onClientError(ClientErrorCode.AUTHENTICATION_FAILURE, ExceptionUtils.toString(throwable));
				}
			}
		};
		client.registerPublishCallback(publisherCallback);
		log.logWithPrefix(Level.INFO, logPrefix, "callback for eventhub is registered");
		return publisherCallback;
	}

	Optional<T> handleAck(Ack ack) {
		String eventId = ack.getId();
		if(!tracingHandler.isTracingAck(ack)) {
			if (eventMap.containsKey(eventId)) {
				if (ack.getStatusCode() == AckStatus.ACCEPTED) {
					return removeAndGetAuditEvent(eventId);
				} else {
					log.logWithPrefix(Level.INFO, logPrefix, "got NACK for event id %s. NACK: %s", eventId, printAck(ack));
					updateEventFailureDetails(eventId, FailCode.BAD_ACK, printAck(ack), null);
				}
			} else { // eventId is not in the cache
				log.logWithPrefix(Level.INFO, logPrefix, "event with id %s was not found in cache", eventId);
			}
		} else { // if is tracing
			tracingHandler.sendCheckpoint(ack);
			log.logWithPrefix(Level.INFO, logPrefix, "event: %s is tracing. removing from cache event", eventId);
			removeElementFromCache(eventId);
		}
		return Optional.empty();
	}

	private void updateEventFailureDetails(String eventId, FailCode status, String description, Throwable e) {
		eventMap.computeIfPresent(eventId,((s, eventContainer) -> {
			eventContainer.setFailReport(status, description, e);
			return eventContainer;}));
	}

	private Optional<T> removeAndGetAuditEvent(String eventId) {
		EventContainer<T> eventContainer = removeElementFromCache(eventId);
		T auditEvent = eventContainer == null ? null : eventContainer.getAuditEvent();
		if (auditEvent == null){
			log.logWithPrefix(Level.INFO, logPrefix,"event %s was already ACKed", eventId);
		}
		return Optional.ofNullable(auditEvent);
	}

	List<T> incrementRetryCountAndSend(List<QueueElement> elements) {
		log.logWithPrefix(Level.INFO, logPrefix,"trying to audit %d events in cache", elements.size());
		List<T> eventsToSend = new ArrayList<>(elements.size());
		List<T> eventsToRemove = new ArrayList<>(elements.size());
		elements.forEach(element -> {
			String messageId = element.getMessageId();
			EventContainer<T> eventContainer = eventMap.get(messageId);
			if(null != eventContainer) {
				if (eventContainer.incrementAndGet() <= retryCount) {
					eventsToSend.add(eventContainer.getAuditEvent());
				} else {
					eventsToRemove.add(eventContainer.getAuditEvent());
				}
			} else{
				log.logWithPrefix(Level.INFO, logPrefix,"Event:{%s} already removed from cache", messageId);
				eventQueue.remove(QueueElement.builder().messageId(messageId).build());
			}
		});

		addToEventhubCacheAndFlush(eventsToSend);
		return eventsToRemove;
	}

	/**
	 * scans the cache every "noAck-threshold" seconds and looks for
	 * messages older then "noAckLimit" and re-sends them.
	 */
	protected synchronized void handleNotAcceptedEvents(){
		long start = System.currentTimeMillis();
		log.logWithPrefix(Level.INFO, logPrefix,"scanning the map for events with no ack: startTime {%d}, map size: {%d}",
				start, eventMap.size());
		List<AuditEventFailReport<T>> failReports = new ArrayList<>();
		retryNotAcceptedEvents(System.currentTimeMillis())
				.forEach(auditEvent -> {
					EventContainer<T> eventContainer = removeElementFromCache(auditEvent.getMessageId());
					if(!tracingHandler.isTracingEvent(auditEvent)){
						AuditEventFailReport<T> auditEventFailReport = eventContainer.getAuditEventFailReport();
						if(auditEventFailReport == null){
							auditEventFailReport = AuditEventFailReport.<T>builder()
									.auditEvent(auditEvent)
									.failureReason(FailCode.NO_ACK)
									.description(generateLogs(logPrefix, NO_ACK_WAS_RECEIVED))
									.build();
						}
						auditEventFailReport.setDescription(generateLogs(logPrefix, auditEventFailReport.getDescription()));
						failReports.add(auditEventFailReport);
						log.logWithPrefix(Level.INFO, logPrefix,"audit event id: %s was removed from cache with" +
								" error %s: %s",auditEvent.getMessageId(), auditEventFailReport.getFailureReason(),
								auditEventFailReport.getDescription());
					} else {
						tracingHandler.sendCheckpoint(auditEvent, LifeCycleEnum.FAIL ,generateLogs(logPrefix, "No more retries"));
					}
				});
		log.logWithPrefix(Level.INFO, logPrefix,"end scanning the map for events{%d} with no ack: endTime {%d}",
				eventMap.size(), System.currentTimeMillis() - start);
		if(!failReports.isEmpty()) {
			callback.onFailure(AuditAsyncResult.<T>builder().failReports(failReports).build());
		}
	}

	List<T> retryNotAcceptedEvents(long start) {
		List<T> eventsForRemove = Lists.newCopyOnWriteArrayList();
		StreamUtils.partitionOf(StreamUtils.takeWhile(eventQueue.stream(), elem -> (start - elem.getTimestamp()) > noAckLimit, false).
				collect(Collectors.toList()), bulkSize)
				.forEach(
						elements -> eventsForRemove.addAll(incrementRetryCountAndSend(elements))
				);
		return eventsForRemove;
	}

	@Override
	protected void sendTracingMessage(){
		tracingHandler.sendInitialCheckpoint().ifPresent(auditTracingEvent -> {
			try {
				audit((T)auditTracingEvent);
			} catch (AuditException e) {
				log.logWithPrefix(Level.INFO, logPrefix, "failed to audit tracing message: " + ExceptionUtils.toString(e));
			}
		});
	}

	@Override
	public synchronized void shutdown() {
		retryExecutorService.shutdownNow();
		List<AuditEventFailReport<T>> failReports = new ArrayList<>();

        eventMap.values().stream().map(EventContainer::getAuditEvent)
                .filter(e -> !tracingHandler.isTracingEvent(e))
                .collect(Collectors.toList())
				.forEach(e ->
						failReports.add(AuditEventFailReport.<T>builder()
						.auditEvent(e)
						.failureReason(FailCode.NO_ACK)
						.description(generateLogs(logPrefix,
								generateLogs(logPrefix, "Client is in shutdown state and cannot handle events")))
						.build()));

		if(!failReports.isEmpty()) {
			callback.onFailure(AuditAsyncResult.<T>builder().failReports(failReports).build());
		}
		super.shutdown();
	}

	public void gracefulShutdown() {
		for (int i = 0; i < retryCount + 2; i++) {
			if (eventMap.isEmpty()) {
				break;
			}
			else {
				ExceptionUtils.swallowException( () -> Thread.sleep(noAckLimit),
						generateLogs(logPrefix, "Failed to sleep during gracefulShutdown"));
			}
		}
        shutdown();
	}

	private String generateLogs(String logPrefix, String s) {
		return String.format("%s%s", logPrefix, s);
	}

}

