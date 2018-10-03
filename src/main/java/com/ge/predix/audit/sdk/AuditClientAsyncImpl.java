package com.ge.predix.audit.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.VersioningException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.ExceptionUtils;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import com.ge.predix.audit.sdk.util.StreamUtils;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.AckStatus;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.client.Client;
import com.ge.predix.eventhub.configuration.PublishAsyncConfiguration;
import com.ge.predix.eventhub.configuration.PublishConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;


/**
 * Created by Martin Saad on 1/31/2017.
 */
@SuppressWarnings("unchecked")
public class AuditClientAsyncImpl extends AbstractAuditClientImpl {

	static final String FAILED_PRECONDITION = "FAILED_PRECONDITION";
	private static final String AUDIT_CACHE_IS_FULL = "audit cache is full";
    private static final String UNAUTHENTICATED = "UNAUTHENTICATED";

	private static CustomLogger log = LoggerUtils.getLogger(AuditClientAsyncImpl.class.getName());

	@Getter
	protected final TypedAuditCallback callback;

	@Setter
	private int queueSize;

	@Getter
	private Map<String, EventContainer> eventMap;

	@Getter
	private LinkedBlockingQueue<QueueElement> eventQueue;

	@Getter
	private ScheduledExecutorService retryExecutorService;

	public AuditClientAsyncImpl(AuditConfiguration configuration, TypedAuditCallback callback, TracingHandler tracingHandler)
			throws EventHubClientException, AuditException {
		super(configuration, tracingHandler);
		log.logWithPrefix(Level.WARNING, logPrefix, "initializing auditClientAsync");

		PublishConfiguration publishConfiguration = new PublishAsyncConfiguration.Builder().build();
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
		setStateAndNotify(AuditCommonClientState.CONNECTED);
	}

	/**
	 * Audit asynchronously a single event.
	 *
	 * @param event - the event to be logged.
	 */
	@Override
	public AuditingResult audit(AuditEvent event) {
		if (event != null) {
			 audit(Lists.newArrayList(event));
		}
		return AuditingResult.emptyResults();
	}

	/**
	 * Audit asynchronously multiple events.
	 *
	 * @param events the events to be logged.
	 */
	@Override
	public synchronized AuditingResult audit(List<AuditEvent> events) {
		throwIfShutdown();
		if(events != null) {
			log.logWithPrefix(Level.INFO, logPrefix, "starting audit %d messages", events.size());
			StreamUtils.partitionOf(events, this.bulkSize).forEach(
					auditEvents -> {
						List<AuditEvent> validEvents = validateEvents(auditEvents);
						addEventsToCache(validEvents);
						addToEventhubCacheAndFlush(validEvents);
					});
		}
		return AuditingResult.emptyResults();
	}

	/******************************************** PRIVATE ***********************************/

	private List<AuditEvent> validateEvents(Collection<AuditEvent> events) {
		log.logWithPrefix(Level.INFO, logPrefix, "Trying to validate %d events", events.size());
		List<AuditEvent> validEvents = new ArrayList<>(events.size());
		events.forEach(event -> {
			try {
				List<ValidatorReport> report = validatorService.validate(event);
				if (!report.isEmpty()) {
					callback.onValidate(event, report);
				} else{
					validEvents.add(event);
				}
			} catch (VersioningException e) {
				callback.onFailure(event, FailReport.VERSION_NOT_SUPPORTED, generateLogs(logPrefix, ExceptionUtils.toString(e)));
			}
		});
		return validEvents;
	}

	private void addToEventhubCache(Collection<AuditEvent> events) {
		log.logWithPrefix(Level.INFO, logPrefix, "adding %d events to EventHub client", events.size());
		events.forEach(event -> {
			try {
				String body = om.writeValueAsString(event);
				client.addMessage(event.getMessageId(), body, null);
			} catch (JsonProcessingException e) {
				log.logWithPrefix(Level.INFO, e, logPrefix, "failed to parse event.");
				updateEventFailureDetails(event.getMessageId(),FailReport.JSON_ERROR, e.toString());
			} catch (EventHubClientException.AddMessageException e) {
				log.logWithPrefix(Level.INFO, e, logPrefix, "failed to add event t eventhub cache");
				updateEventFailureDetails(event.getMessageId(), FailReport.ADD_MESSAGE_ERROR, e.toString());
			}
		});
	}

	private  synchronized void innerReconnect(){
		try {
			reconnect();
		} catch (EventHubClientException e) {
			String err =  ExceptionUtils.toString(e);
			setStateAndNotify(AuditCommonClientState.DISCONNECTED);
			callback.onFailure(FailReport.RECONNECT_FAILURE, err);
			log.logWithPrefix(Level.WARNING, e, logPrefix,"reconnect failed in reconnectStrategy");
		}
	}

	void addToEventhubCacheAndFlush(Collection<AuditEvent> auditToSend){
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

	EventContainer removeElementFromCache(String messageId){
		EventContainer eventContainer = eventMap.remove(messageId);
		eventQueue.remove(QueueElement.builder().messageId(messageId).build());
		return eventContainer;
	}

	void removeLatestElementFromCache(){
		log.logWithPrefix(Level.INFO, logPrefix,"queue size reached to limit of %d elements. removing the oldest event", queueSize);
		String messageId = eventQueue.poll().getMessageId();
		EventContainer eventContainer = eventMap.remove(messageId);
		callback.onFailure(eventContainer.getAuditEvent(), FailReport.CACHE_IS_FULL, generateLogs(logPrefix, AUDIT_CACHE_IS_FULL));
	}

	void addEventsToCache(Collection<AuditEvent> events) {
		//if queue had reached its limit - remove the oldest event and callback as failure
		events.forEach(event -> {
			removeEventsWhenLimitReached();
			addEventToCache(event);
			log.logWithPrefix(Level.INFO, logPrefix, "event with id %s was added successfully to cache eventMap size: %d, eventQueue size: %d",
					event.getMessageId(), eventMap.size(), eventQueue.size());
		});
	}

	private void removeEventsWhenLimitReached() {
		if (eventQueue.size() >= queueSize){
			removeLatestElementFromCache();
		}
	}

	private void addEventToCache(AuditEvent event) {
		String messageId = event.getMessageId();
		eventMap.put(messageId, EventContainer.builder().auditEvent(event.clone()).build());
		eventQueue.offer(QueueElement.builder().messageId(messageId).build());
	}

	Client.PublishCallback handleEventHubCallback() throws EventHubClientException {
		log.logWithPrefix(Level.WARNING, logPrefix,"registering eventhub callback");
		Client.PublishCallback callback = new Client.PublishCallback() {

			@Override
			public void onAck(List<Ack> list) {
				log.logWithPrefix(Level.INFO, logPrefix,"new callback onAck from eventHub");
				if ((list != null) && !list.isEmpty()){
					setStateAndNotify(AuditCommonClientState.ACKED);
					list.forEach(ack -> handleAck(ack));
				} else{
					log.logWithPrefix(Level.INFO, logPrefix,"ack list is empty");
				}
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.logWithPrefix(Level.INFO, throwable, logPrefix, "new callback onFailure from eventHub");
				if (throwable.getMessage() != null && throwable.getMessage().contains(FAILED_PRECONDITION)) {
					setStateAndNotify(AuditCommonClientState.DISCONNECTED);
					getCallback().onFailure(FailReport.STREAM_IS_CLOSE, throwable.toString());
				}
				else if(throwable.getMessage() != null && throwable.getMessage().contains(UNAUTHENTICATED)){
					getCallback().onFailure(FailReport.AUTHENTICATION_FAILURE, throwable.toString());
				}
			}
		};
		client.registerPublishCallback(callback);
		log.info(generateLogs(logPrefix,"callback for eventhub is registered"));
		return callback;
	}

	void handleAck(Ack ack) {
		String eventId = ack.getId();
		if(!tracingHandler.isTracingAck(ack)){
			if (eventMap.containsKey(eventId)) {
				if (ack.getStatusCode() == AckStatus.ACCEPTED){
					removeElementAndNotifyOnSuccess(eventId);
				} else{
					log.logWithPrefix(Level.INFO, logPrefix, "got NACK for event id %s. NACK: %s", eventId, printAck(ack));
					updateEventFailureDetails(eventId,FailReport.BAD_ACK, printAck(ack));
				}
			} else{ //eventId is not in the cache
				log.logWithPrefix(Level.INFO, logPrefix, "event with id %s was not found in cache", eventId);
			}
		} else { // if is tracing
			tracingHandler.sendCheckpoint(ack);
			log.logWithPrefix(Level.INFO, logPrefix, "event: %s is tracing. removing from cache event",eventId);
			removeElementFromCache(eventId);
		}
	}

	private void updateEventFailureDetails(String eventId, FailReport status, String description) {
		eventMap.computeIfPresent(eventId,((s, eventContainer) -> {
            eventContainer.setFailReport(status, description);
            return eventContainer;}));
	}

	private void removeElementAndNotifyOnSuccess(String eventId) {
		EventContainer eventContainer = removeElementFromCache(eventId);
		AuditEvent auditEvent = eventContainer == null? null : eventContainer.getAuditEvent();
		if (auditEvent != null){
			log.logWithPrefix(Level.INFO, logPrefix,"ack from message id: %s was successful. notifying the user onSuccess"
					, auditEvent.getMessageId());
			callback.onSuccees(auditEvent);
		} else{
			log.logWithPrefix(Level.INFO, logPrefix,"event %s was already ACKed", eventId);
		}
	}

	List<AuditEvent> incrementRetryCountAndSend(List<QueueElement> elements) {
		log.logWithPrefix(Level.INFO, logPrefix,"trying to audit %d events in cache", elements.size());
		List<AuditEvent> eventsToSend = new ArrayList<>(elements.size());
		List<AuditEvent> eventsToRemove = new ArrayList<>(elements.size());
		elements.forEach(element -> {
			String messageId = element.getMessageId();
			EventContainer eventContainer = eventMap.get(messageId);
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
		log.logWithPrefix(Level.INFO, logPrefix,"scanning the map for events{%d} with no ack: startTime {%d}",
				eventMap.size(), start);
		retryNotAcceptedEvents(System.currentTimeMillis())
				.forEach(auditEvent -> {
					EventContainer eventContainer = removeElementFromCache(auditEvent.getMessageId());
					if(!tracingHandler.isTracingEvent(auditEvent)){
						AuditEventFailReport auditEventFailReport = eventContainer.getAuditEventFailReport();
						FailReport failReport = (auditEventFailReport == null? FailReport.NO_ACK : auditEventFailReport.getFailureReason());
						String description = (auditEventFailReport == null? NO_ACK_WAS_RECEIVED : auditEventFailReport.getDescription());
						callback.onFailure(auditEvent, failReport, generateLogs(logPrefix, description));
						log.logWithPrefix(Level.INFO, logPrefix,"audit event id: %s was removed from cache with error %s: %s",auditEvent.getMessageId(),failReport,description);
					}
					else{
						tracingHandler.sendCheckpoint(auditEvent, LifeCycleEnum.FAIL ,generateLogs(logPrefix, "No more retries"));
					}
				});
		log.logWithPrefix(Level.INFO, logPrefix,"end scanning the map for events{%d} with no ack: endTime {%d}",
				eventMap.size(), System.currentTimeMillis() - start);
	}

	List<AuditEvent> retryNotAcceptedEvents(long start) {
		List<AuditEvent> eventsForRemove = Lists.newCopyOnWriteArrayList();
		StreamUtils.partitionOf(StreamUtils.takeWhile(eventQueue.stream(), elem -> (start - elem.getTimestamp()) > noAckLimit, false).
				collect(Collectors.toList()), bulkSize)
				.forEach(
						elements -> eventsForRemove.addAll(incrementRetryCountAndSend(elements))
				);
		return eventsForRemove;
	}

	@Override
	protected void sendTracingMessage(){{
		tracingHandler.sendInitialCheckpoint().ifPresent(this::audit);
        }
	}

	@Override
	public synchronized void shutdown() {
		retryExecutorService.shutdownNow();
        eventMap.values().stream().map(EventContainer::getAuditEvent)
                .filter(e -> !tracingHandler.isTracingEvent(e))
                .collect(Collectors.toList())
                .forEach(e -> callback.onFailure(e, FailReport.NO_MORE_RETRY, generateLogs(logPrefix, "Client is in shutdown state and cannot handle events")));
		super.shutdown();
	}

	public void gracefulShutdown() {
		for (int i = 0; i < retryCount + 2; i++) { //1 more to clean events with timestamp > now-noAckLimit
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

