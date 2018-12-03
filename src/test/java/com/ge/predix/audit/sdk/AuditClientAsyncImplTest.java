package com.ge.predix.audit.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.ReconnectMode;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.message.AuditTracingEvent;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSender;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSenderImpl;
import com.ge.predix.audit.sdk.util.ExceptionUtils;
import com.ge.predix.audit.sdk.util.ReflectionUtils;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.client.Client;
import com.ge.predix.eventhub.configuration.EventHubConfiguration;
import com.ge.predix.eventhub.configuration.PublishConfiguration;
import com.ge.predix.eventhub.stub.Ack;
import com.ge.predix.eventhub.stub.AckStatus;
import com.ge.predix.eventhub.stub.Timestamp;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Spy;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ge.predix.audit.sdk.AuditClientAsyncImpl.STREAM_CLOSED;
import static com.ge.predix.audit.sdk.AuditClientAsyncImpl.NO_ACK_WAS_RECEIVED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AuditClientAsyncImplTest {


    private static Logger log = Logger.getLogger(AuditClientAsyncImplTest.class.getName());

    private static AuditConfiguration goodConfiguration;
    private static AuditConfiguration bulkConfiguration;
    private static ObjectMapper om;
    private static int queueSize = 20000;
    private TracingMessageSender tracingMessageSender;
    public static String eventhubZoneId = UUID.randomUUID().toString();


    AuditEventV2 auditEvent_1 = AuditEventV2.builder()
            .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
            .eventType(AuditEnums.EventType.STARTUP_EVENT)
            .classifier(AuditEnums.Classifier.FAILURE)
            .publisherType(AuditEnums.PublisherType.APP_SERVICE)
            .build();
    AuditEventV2 auditEvent_2 = AuditEventV2.builder()
            .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
            .eventType(AuditEnums.EventType.STARTUP_EVENT)
            .classifier(AuditEnums.Classifier.FAILURE)
            .publisherType(AuditEnums.PublisherType.APP_SERVICE)
            .build();
    AuditEventV2 auditEvent_3 = AuditEventV2.builder()
            .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
            .eventType(AuditEnums.EventType.STARTUP_EVENT)
            .classifier(AuditEnums.Classifier.FAILURE)
            .publisherType(AuditEnums.PublisherType.APP_SERVICE)
            .build();
    AuditEventV2 auditEvent_4 = AuditEventV2.builder()
            .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
            .eventType(AuditEnums.EventType.STARTUP_EVENT)
            .classifier(AuditEnums.Classifier.SUCCESS)
            .publisherType(AuditEnums.PublisherType.APP_SERVICE)
            .build();

    String id_1 = auditEvent_1.getMessageId();
    String id_2 = auditEvent_2.getMessageId();
    String id_3 = auditEvent_3.getMessageId();
    String id_4 = auditEvent_4.getMessageId();

    EventContainer<AuditEventV2> eventContainer_1 = new EventContainer<>(auditEvent_1);
    EventContainer<AuditEventV2> eventContainer_2 = new EventContainer<>(auditEvent_2);
    EventContainer<AuditEventV2> eventContainer_3 = new EventContainer<>(auditEvent_3);
    QueueElement queueElement_1 = new QueueElement(auditEvent_1.getMessageId(), System.currentTimeMillis());
    QueueElement queueElement_2 = new QueueElement(auditEvent_2.getMessageId(), System.currentTimeMillis() - 1000); //older
    QueueElement queueElement_3 = new QueueElement(auditEvent_3.getMessageId(), System.currentTimeMillis());

    @Spy
    ExponentialReconnectStrategy exponentialReconnectStrategy;


    @Before
    public void init() {
        tracingMessageSender = mock(TracingMessageSenderImpl.class);
        om = new ObjectMapper();

        goodConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId(new UUID(1,1).toString())
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .tracingInterval(9000)
                .tracingUrl("http://localhost:443/tracing")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(false)
                .traceEnabled(false)
                .retryIntervalMillis(2000)
                .build();

        bulkConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .tracingInterval(300)
                .tracingToken("token")
                .tracingUrl("http://localhost:443/tracing")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(true)
                .build();
    }

    @Test
    public void initGoodTest() throws EventHubClientException, AuditException {
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, new TestHelper(), new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
    }

    @Test
    public void bulkSizeTest() throws EventHubClientException, AuditException {
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, new TestHelper(), new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        int expectedBulk = 1;
        auditClientAsyncImpl.setBulkSize(expectedBulk);
        auditClientAsyncImpl.setQueueSize(queueSize);
        assertThat(auditClientAsyncImpl.getBulkSize(), is(expectedBulk));
    }

    @Test
    public void removeElementFromCacheTest_elementIsRemoved() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);

        auditClientAsyncImpl.getEventMap().put(id_1, eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);

        assertThat(auditClientAsyncImpl.removeElementFromCache(id_1).getAuditEvent(), is(auditEvent_1));
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(0));
        assertThat(auditClientAsyncImpl.getEventQueue().size(), is(0));
    }

    @Test
    public void automaticReconnectDefaultEngineTest_exponentialReconnectStrategy() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        assertThat(goodConfiguration.getReconnectMode(),is(ReconnectMode.AUTOMATIC));
        assertThat(auditClientAsyncImpl.getReconnectEngine(),is(instanceOf(ExponentialReconnectStrategy.class)));
    }

    @Test
    public void removeLatestElementFromCacheTest() throws EventHubClientException, AuditException {
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, null, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.getEventMap().put(id_1, eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);
        auditClientAsyncImpl.getEventMap().put(id_2, eventContainer_2);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_2);

        Optional<AuditEventFailReport<AuditEventV2>> auditEventFailReport = auditClientAsyncImpl.removeLatestElementFromCache();
        assertThat(auditEventFailReport.get().getFailureReason(), is(FailCode.CACHE_IS_FULL));
        assertThat(auditEventFailReport.get().getAuditEvent(), is(auditEvent_1));
    }

    @Test
    public void addEventsToCacheTest_eventsAreAdded() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        List<AuditEventV2> events = Lists.<AuditEventV2>newArrayList(auditEvent_1, auditEvent_2);
        auditClientAsyncImpl.addEventsToCache(events);
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(2));
        assertThat(auditClientAsyncImpl.getEventQueue().size(), is(2));
    }


    @Test
    public void addEventsToCacheTest_cacheIsFullAndDuplicateEvents() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(2);
        List<AuditEventV2> events = Lists.newArrayList(auditEvent_1, auditEvent_1 ,auditEvent_2 , auditEvent_3 );
        auditClientAsyncImpl.addEventsToCache(events);
        assertThat(cb.failReports.size(), is(2));
        Set<FailCode> failCodes = new HashSet<>();
        cb.failReports.stream().forEach(f -> failCodes.add(f.getFailureReason()));
        assertTrue(failCodes.contains(FailCode.DUPLICATE_EVENT));
        assertTrue(failCodes.contains(FailCode.CACHE_IS_FULL));
    }


    @Test
    public void addEventsToCache_EventQueueBiggerThanQueueSize_lastElementISRemovedAndNotified() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(1);

        auditClientAsyncImpl.addEventsToCache(Arrays.asList(auditEvent_1, auditEvent_2));
        assertThat(cb.getFailures(), is(1));
        assertThat(cb.lastFailureEvent, is(auditEvent_1));
        auditClientAsyncImpl.addEventsToCache(Arrays.asList(auditEvent_3));
        assertThat(cb.getFailures(), is(2));
        assertThat(cb.lastFailureEvent, is(auditEvent_2));
    }

    @Test
    public void addToEventhubAndFlushTest() throws EventHubClientException, AuditException {
        Client client = mock(Client.class);
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setOm(om);
        auditClientAsyncImpl.setClient(client);

        List<AuditEventV2> events = Lists.newArrayList(auditEvent_1, auditEvent_2, auditEvent_3);

        auditClientAsyncImpl.addToEventhubCacheAndFlush(events);
        verify(client, times(1)).flush();
    }


    @Test
    public void innerReconnect_reconnectFailureTest() throws EventHubClientException, AuditException {
        Client client = mock(Client.class);
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setOm(om);
        auditClientAsyncImpl.setClient(client);
        doThrow(new EventHubClientException("test")).when(client).reconnect();
        List<AuditEventV2> events = Lists.newArrayList(auditEvent_1, auditEvent_2, auditEvent_3);

        auditClientAsyncImpl.innerReconnect();

        assertThat(cb.lastClientErrorCode, is(ClientErrorCode.RECONNECT_FAILURE));
    }


    @Test
    public void auditAcceptedTest_shouldNotifyOnSuccess() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = spy(new TestHelper<>());
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        auditClientAsyncImpl.setQueueSize(2);
        when(client.flush())
                .thenAnswer((c) -> {
                    auditClientAsyncImpl.handleEventHubCallback()
                            .onAck(Arrays.asList(Ack.newBuilder()
                                                    .setId(id_1)
                                                    .setStatusCode(AckStatus.ACCEPTED)
                                                    .build(),
                                                Ack.newBuilder()
                                                    .setId(id_2)
                                                    .setStatusCode(AckStatus.ACCEPTED)
                                                    .build()));
                    return null;
                })
                .thenReturn(new ArrayList<>());

        auditClientAsyncImpl.audit(Arrays.asList(auditEvent_1,auditEvent_2));

        assertThat(cb.getSuccessCount(), is(2));
        assertThat(cb.lastSuccessEventsBatch, is(Arrays.asList(auditEvent_1,auditEvent_2)));
    }

    @Test
    public void handleAck_eventIdIsNotInTheCacheTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));

        auditClientAsyncImpl.getEventMap().put("1", eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);
        Ack ack = Ack.newBuilder().setId(id_2).setStatusCode(AckStatus.ACCEPTED).build();

        auditClientAsyncImpl.handleAck(ack);
        assertThat(cb.getSuccessCount(), is(0));
        assertThat(cb.getFailureCount(), is(0));

    }


    @Test
    public void handleAckNotAccepted_nothingShouldBeNotified() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));

        auditClientAsyncImpl.getEventMap().put("1", eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);
        Ack ack = Ack.newBuilder().setId("1").setStatusCode(AckStatus.BAD_REQUEST).build();

        auditClientAsyncImpl.handleAck(ack);
        assertThat(cb.getSuccessCount(), is(0));
        assertThat(cb.getFailureCount(), is(0));
    }

    @Test
    public void handleAckNotAccepted_AckMessageIsReturned() throws EventHubClientException, AuditException, InterruptedException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        int retryCount = 0;
        long moreThanIntervalMillis = 4000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setRetryCount(retryCount);

        auditClientAsyncImpl.getEventMap().put(id_1, eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);
        Ack ack = Ack.newBuilder().setId(id_1).setStatusCode(AckStatus.BAD_REQUEST).setDesc("ack description").setTimestamp(Timestamp.newBuilder().build()).build();
        auditClientAsyncImpl.handleAck(ack);

        Thread.sleep(moreThanIntervalMillis);
        assertThat(cb.getFailureCount(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.BAD_ACK));
        System.out.println("ack: "+auditClientAsyncImpl.printAck(ack));
        assert (cb.lastFailureDescription.contains(auditClientAsyncImpl.printAck(ack)));
    }

    @Test
    public void handleAckNotAcceptedTwice_secondAckMessageIsReturned() throws EventHubClientException, AuditException, InterruptedException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        int retryCount = 1;
        long moreThanIntervalMillis = 6000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setRetryCount(retryCount);

        auditClientAsyncImpl.getEventMap().put(id_1, eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);
        Ack ack = Ack.newBuilder().setId(id_1).setStatusCode(AckStatus.BAD_REQUEST).build();
        Ack secondAck = Ack.newBuilder().setId(id_1).setStatusCode(AckStatus.REQUEST_TOO_LARGE).build();
        auditClientAsyncImpl.handleAck(ack);
        auditClientAsyncImpl.handleAck(secondAck);

        Thread.sleep(moreThanIntervalMillis);
        assertThat(cb.getFailureCount(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.BAD_ACK));
        assert (cb.lastFailureDescription.contains(auditClientAsyncImpl.printAck(secondAck)));
    }
    @Test
    public void auditMessageFailToAddToEh_errorIsReturnedInOnFailure() throws EventHubClientException, AuditException, InterruptedException {
        int moreThanMaxRetry = 6000;
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        int retryCount = 1;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setRetryCount(retryCount);
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        when(client.addMessage(any(), any(), any())).thenThrow(new EventHubClientException.AddMessageException("add error"));
        auditClientAsyncImpl.audit(auditEvent_1);
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(1));
        try {
            Thread.sleep(moreThanMaxRetry);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(0));
        assertThat(cb.getFailureCount(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.ADD_MESSAGE_ERROR));
        assertThat(cb.lastFailureDescription, containsString("failed to add the event to eventhub cache"));
        assertThat(ExceptionUtils.toString(cb.lastFailureExcepion), containsString("add error"));
    }


    @Test
    public void handleAckNotAccepted_noAckIsObtained_defaultIsReturned() throws EventHubClientException, AuditException, InterruptedException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long moreThanIntervalMillis = 2500;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        int retryCount = 0;
        auditClientAsyncImpl.setRetryCount(retryCount);
        auditClientAsyncImpl.setStateAndNotify(AuditCommonClientState.DISCONNECTED);
        auditClientAsyncImpl.getEventMap().put(id_1, eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);

        Thread.sleep(moreThanIntervalMillis);
        assertThat(cb.getFailureCount(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.NO_ACK));
        assertThat(cb.lastFailureDescription, containsString(NO_ACK_WAS_RECEIVED));
    }


    @Test
    public void testIncrementRetryCountAndSend_retryCountUnderMaximumTest_retryShouldBeAttempted() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setOm(om);
        auditClientAsyncImpl.setRetryCount(3);
        //to avoid the running thread
        auditClientAsyncImpl.setNoAckLimit(5000);

        auditClientAsyncImpl.getEventMap().put(id_1, eventContainer_1);
        auditClientAsyncImpl.getEventQueue().offer(queueElement_1);

        auditClientAsyncImpl.incrementRetryCountAndSend(Lists.newArrayList(queueElement_1));
        assertThat(eventContainer_1.getNumberOfRetriesMade().get(), is(1));
        auditClientAsyncImpl.incrementRetryCountAndSend(Lists.newArrayList(queueElement_1));
        assertThat(eventContainer_1.getNumberOfRetriesMade().get(), is(2));

    }

    @Test
    public void incrementRetryCountAndSend_eventReachedMaxRetry_eventIsReturnedInListToRemove() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setOm(om);
        int retryCount = 2;
        auditClientAsyncImpl.setRetryCount(retryCount);

        AuditEvent auditEvent1 = mock(AuditEvent.class);
        when(auditEvent1.getMessageId()).thenReturn("1");
        EventContainer eventContainer = mock(EventContainer.class);
        when(eventContainer.getAuditEvent()).thenReturn(auditEvent1);
        when(eventContainer.incrementAndGet()).thenReturn(retryCount + 1);

        QueueElement element = QueueElement.builder().messageId("1").build();
        auditClientAsyncImpl.getEventMap().put("1", eventContainer);
        auditClientAsyncImpl.getEventQueue().offer(element);
        assertThat(auditClientAsyncImpl.incrementRetryCountAndSend(Lists.newArrayList(element)).size(), is(1));
    }

    @Test
    public void incrementRetryCountAndSend_EventIsNotInMap_eventIsRemovedFromMap() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setOm(om);
        auditClientAsyncImpl.setRetryCount(2);

        AuditEvent auditEvent1 = mock(AuditEvent.class);
        when(auditEvent1.getMessageId()).thenReturn("1");
        EventContainer eventContainer = mock(EventContainer.class);

        QueueElement element = QueueElement.builder().messageId("1").build();
        when(eventContainer.getAuditEvent()).thenReturn(auditEvent1);
        when(eventContainer.incrementAndGet()).thenReturn(2);

        assertThat(auditClientAsyncImpl.incrementRetryCountAndSend(Lists.newArrayList(element)).size(), is(0));
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(0));
    }

    /*
     *
     * should re-send only messages 1 and 2 since they passed the "noAckLimit"
     * @throws EventHubClientException
     */
    @Test
    public void handleNoAckTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);
        Client client = mock(Client.class);
        when(client.addMessage(any(), any(), any())).thenReturn(client);
        auditClientAsyncImpl.setClient(client);

        DateTime currentTimestamp = DateTime.now();
        String auditEvent1MessageId = auditEvent_1.getMessageId();
        EventContainer<AuditEventV2> eventContainer1 = new EventContainer<>(auditEvent_1);
        QueueElement element1 = mock(QueueElement.class);
        when(element1.getMessageId()).thenReturn(auditEvent1MessageId);
        when(element1.getTimestamp()).thenReturn(currentTimestamp.minusSeconds(7).getMillis());

        //audit event2 should not be re-send
        String auditEvent2MessageId = auditEvent_2.getMessageId();
        EventContainer eventContainer2 = new EventContainer<>(auditEvent_2);
        QueueElement element2 = mock(QueueElement.class);
        when(element2.getMessageId()).thenReturn(auditEvent2MessageId);
        when(element2.getTimestamp()).thenReturn(currentTimestamp.minusSeconds(6).getMillis());

        String auditEvent3MessageId = auditEvent_3.getMessageId();
        EventContainer eventContainer3 = new EventContainer<>(auditEvent_3);
        QueueElement element3 = mock(QueueElement.class);
        when(element3.getMessageId()).thenReturn(auditEvent3MessageId);
        when(element3.getTimestamp()).thenReturn(currentTimestamp.minusSeconds(1).getMillis());

        auditClientAsyncImpl.getEventMap().put(auditEvent1MessageId, eventContainer1);
        auditClientAsyncImpl.getEventMap().put(auditEvent2MessageId, eventContainer2);
        auditClientAsyncImpl.getEventMap().put(auditEvent3MessageId, eventContainer3);
        auditClientAsyncImpl.getEventQueue().offer(element1);
        auditClientAsyncImpl.getEventQueue().offer(element2);
        auditClientAsyncImpl.getEventQueue().offer(element3);

        auditClientAsyncImpl.handleNotAcceptedEvents();
        verify(client, times(2)).addMessage(any(), any(), any());
        assertThat(cb.getFailureCount(), is(0));
        assertThat(cb.getSuccessCount(), is(0));
    }


    /**
     * should not duplicate events in the cache queue
     *
     */
    @Test
    public void noDuplicateMessageIdInCacheQueueTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);
        Client client = mock(Client.class);
        when(client.addMessage(any(), any(), any())).thenReturn(client);
        auditClientAsyncImpl.setClient(client);

        List<AuditEventV2> events = new ArrayList<>();
        events.add(auditEvent_1);
        events.add(auditEvent_1.clone());

        auditClientAsyncImpl.addEventsToCache(events);

        assertThat(cb.getFailures(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.DUPLICATE_EVENT));
        assertThat(auditClientAsyncImpl.getEventQueue().size() , is(1));
        assertEquals("event queue and event map should be aligned" +
                " in the event count" , auditClientAsyncImpl.getEventMap().size() , auditClientAsyncImpl.getEventQueue().size());
    }


    /**
     * should not re-send message 1 since it didn't passed the "noAckLimit"
     *
     * @throws EventHubClientException
     */
    @Test
    public void handleNoAckForOneMessageTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);
        Client client = mock(Client.class);
        when(client.addMessage(any(), any(), any())).thenReturn(client);
        auditClientAsyncImpl.setClient(client);

        DateTime currentTimestamp = DateTime.now();
        String auditEvent1MessageId = auditEvent_1.getMessageId();
        EventContainer<AuditEventV2> eventContainer1 = new EventContainer<>(auditEvent_1);
        QueueElement element1 = mock(QueueElement.class);
        when(element1.getMessageId()).thenReturn(auditEvent1MessageId);
        when(element1.getTimestamp()).thenReturn(currentTimestamp.minusSeconds(1).getMillis());

        auditClientAsyncImpl.getEventMap().put(auditEvent1MessageId, eventContainer1);
        auditClientAsyncImpl.getEventQueue().offer(element1);

        auditClientAsyncImpl.handleNotAcceptedEvents();
        verify(client, times(0)).addMessage(any(), any(), any());
    }


    /**
     * should re-send only messages 1 and 3 since they passed the "noAckLimit"
     *
     * @throws EventHubClientException
     */
    @Test
    public void handleNoAckForTwoMessages_noMessageShouldBeResent() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);
        Client client = mock(Client.class);
        when(client.addMessage(any(), any(), any())).thenReturn(client);
        auditClientAsyncImpl.setClient(client);

        DateTime currentTimestamp = DateTime.now();
        String auditEvent1MessageId = auditEvent_1.getMessageId();
        EventContainer<AuditEventV2> eventContainer1 = new EventContainer<>(auditEvent_1);
        QueueElement element1 = mock(QueueElement.class);
        when(element1.getMessageId()).thenReturn(auditEvent1MessageId);
        when(element1.getTimestamp()).thenReturn(currentTimestamp.minusSeconds(1).getMillis());

        //audit event2 should not be re-send
        String auditEvent2MessageId = auditEvent_2.getMessageId();
        EventContainer eventContainer2 = new EventContainer<>(auditEvent_2);
        QueueElement element2 = mock(QueueElement.class);
        when(element2.getMessageId()).thenReturn(auditEvent2MessageId);
        when(element2.getTimestamp()).thenReturn(currentTimestamp.minusSeconds(1).getMillis());

        auditClientAsyncImpl.getEventMap().put(auditEvent1MessageId, eventContainer1);
        auditClientAsyncImpl.getEventMap().put(auditEvent2MessageId, eventContainer2);
        auditClientAsyncImpl.getEventQueue().offer(element1);
        auditClientAsyncImpl.getEventQueue().offer(element2);

        auditClientAsyncImpl.handleNotAcceptedEvents();
        verify(client, times(0)).addMessage(any(), any(), any());
    }

    @Test
    public void handleEventHubCallbackNoAcks_noAcksInList_noNotifications() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);

        Client.PublishCallback callback = auditClientAsyncImpl.handleEventHubCallback();
        callback.onAck(new ArrayList<>());
        assertThat(cb.getValidateCount(), is(0));
        assertThat(cb.getFailures(), is(0));
        assertThat(cb.getSuccessCount(), is(0));
    }

    @Test
    public void handleEventHubCallbackThrowableTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);

        Client.PublishCallback callback = auditClientAsyncImpl.handleEventHubCallback();
        callback.onFailure(new Throwable());
        assertThat(cb.getValidateCount(), is(0));
        assertThat(cb.getFailures(), is(0));
        assertThat(cb.getSuccessCount(), is(0));
    }

    @Test
    public void handleEventHubCallbackStreamClosedTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);

        Client.PublishCallback callback = auditClientAsyncImpl.handleEventHubCallback();
        callback.onFailure(new Throwable(STREAM_CLOSED));

        assertThat(cb.lastClientErrorCode, is(ClientErrorCode.STREAM_IS_CLOSE));
    }

    @Test
    public void handleEventHubCallbackThrowablePreConditionTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        long noAckLimit = 5000;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);

        Client.PublishCallback callback = auditClientAsyncImpl.handleEventHubCallback();
        callback.onFailure(new Throwable(STREAM_CLOSED));
        assertThat(cb.getValidateCount(), is(0));
        assertThat(cb.getFailures(), is(1));
        assertThat(cb.getSuccessCount(), is(0));
    }

    @Test
    public void handleEventHubCallbackWithAcksHighLoadTest() throws EventHubClientException, AuditException, InterruptedException {
        int numberOfEvents = 1000;
        long noAckLimit = 2000;
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.setQueueSize(queueSize);
        auditClientAsyncImpl.setNoAckLimit(noAckLimit);
        auditClientAsyncImpl.setOm(om);
        auditClientAsyncImpl.setRetryCount(0);
        Client.PublishCallback callback = auditClientAsyncImpl.handleEventHubCallback();
        List<Ack> acks = Lists.newArrayList();

        for (int i = 0; i < numberOfEvents; i++) {
            AuditEvent successEvent = AuditEventV2.builder()
                    .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                    .eventType(AuditEnums.EventType.STARTUP_EVENT)
                    .classifier(AuditEnums.Classifier.SUCCESS)
                    .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                    .build();
            EventContainer successEventContainer = new EventContainer<>(successEvent);
            QueueElement successElement = QueueElement.builder().messageId(successEvent.getMessageId()).build();
            auditClientAsyncImpl.getEventMap().put(successEvent.getMessageId(), successEventContainer);
            auditClientAsyncImpl.getEventQueue().offer(successElement);
            acks.add(Ack.newBuilder().setId(successEvent.getMessageId()).setStatusCode(AckStatus.ACCEPTED).build());
            i++;
            AuditEvent failEvent = AuditEventV2.builder()
                    .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                    .eventType(AuditEnums.EventType.STARTUP_EVENT)
                    .classifier(AuditEnums.Classifier.SUCCESS)
                    .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                    .build();
            EventContainer failEventContainer = new EventContainer<>(failEvent);
            QueueElement failElement = QueueElement.builder().messageId(failEvent.getMessageId()).build();
            auditClientAsyncImpl.getEventMap().put(failEvent.getMessageId(), failEventContainer);
            auditClientAsyncImpl.getEventQueue().offer(failElement);
            acks.add(Ack.newBuilder().setId(failEvent.getMessageId()).setStatusCode(AckStatus.BAD_REQUEST).build());
        }

        new Thread(() -> callback.onAck(acks)).start();
        Thread.sleep(6000);
        assertThat(cb.getValidateCount(), is(0));
        assertThat(cb.getFailures(), is(numberOfEvents / 2));
        assertThat(cb.getSuccessCount(), is(numberOfEvents / 2));
    }

    @Test
    public void testRemoveFromQueue() throws InterruptedException {
        LinkedBlockingQueue<QueueElement> eventQueue = Queues.newLinkedBlockingQueue();
        String eventId = "1";
        eventQueue.offer(QueueElement.builder().messageId(eventId).build());
        Thread.sleep(100);
        eventQueue.remove(QueueElement.builder().messageId(eventId).build());
        assertThat(eventQueue.size(), is(0));
    }

    @Test
    public void testLimitQueue_eventsRetryMaxedOut_eventsAreListedToBeRemoved() throws InterruptedException, EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        //AuditClientAsync auditClientAsync = Mockito.spy(auditClientAsyncReal);
        auditClientAsyncImpl.setNoAckLimit(50);
        auditClientAsyncImpl.setBulkSize(10);
        auditClientAsyncImpl.setStateAndNotify(AuditCommonClientState.CONNECTED);

        Client mockedClient = mock(Client.class);
        when(mockedClient.addMessage(any())).thenReturn(mockedClient);
        when(mockedClient.flush()).thenReturn(null);
        auditClientAsyncImpl.setClient(mockedClient);


        for (int i = 1; i <= 100; i++) {
            EventContainer eventContainer = new EventContainer<>(cloneAndUpdateindexes(auditEvent_4, i));
            eventContainer.incrementAndGet();
            eventContainer.incrementAndGet();
            auditClientAsyncImpl.getEventMap().put("" + i,eventContainer);
            auditClientAsyncImpl.getEventQueue().offer(QueueElement.builder().messageId("" + i).timestamp(i).build());
        }


        List<AuditEventV2> list = auditClientAsyncImpl.retryNotAcceptedEvents(70);
        assertThat(list.size(), is(19));
        verify(mockedClient, never()).flush();
    }

    @Test
    public void testLimitQueue_eventsRetryIsNotMaxedOut_eventsAreSent() throws InterruptedException, EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        //AuditClientAsync auditClientAsync = Mockito.spy(auditClientAsyncReal);
        auditClientAsyncImpl.setNoAckLimit(50);
        auditClientAsyncImpl.setBulkSize(10);
        auditClientAsyncImpl.setStateAndNotify(AuditCommonClientState.CONNECTED);

        Client mockedClient = mock(Client.class);
        when(mockedClient.addMessage(any())).thenReturn(mockedClient);
        when(mockedClient.flush()).thenReturn(null);
        auditClientAsyncImpl.setClient(mockedClient);


        for (int i = 1; i <= 100; i++) {
            EventContainer eventContainer = new EventContainer<>(cloneAndUpdateindexes(auditEvent_4, i));
            eventContainer.incrementAndGet();
            auditClientAsyncImpl.getEventMap().put("" + i, eventContainer);
            auditClientAsyncImpl.getEventQueue().offer(QueueElement.builder().messageId("" + i).timestamp(i).build());
        }


        List<AuditEventV2> list = auditClientAsyncImpl.retryNotAcceptedEvents(70);
        assertThat(list.size(), is(0));
        verify(mockedClient, times(2)).flush();
    }


    @Test
    public void reconnectClientTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        auditClientAsyncImpl.reconnect();
        assertThat(auditClientAsyncImpl.getAuditCommonClientState(), is(AuditCommonClientState.CONNECTED));
        verify(client).reconnect();
    }

    @Test(expected = IllegalStateException.class)
    public void testReconnectClient_clientIsShutdown_throws() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        auditClientAsyncImpl.shutdown();
        auditClientAsyncImpl.reconnect();

    }

    @Test
    public void TestTrace_traceHandlerIsNull_nothingShouldHappen() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandleEmptyImpl());
        auditClientAsyncImpl.trace();
        auditClientAsyncImpl.sendTracingMessage();
    }

    @Test
    public void TestTrace_traceHandlerNotNull_traceShouldWork() throws EventHubClientException, AuditException, InterruptedException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        Client mockedClient = mock(Client.class);
        TracingMessageSenderImpl mockTracingMessageSender = mock(TracingMessageSenderImpl.class);
        TracingHandlerImpl tracingHandler = new TracingHandlerImpl(goodConfiguration, "async");
        tracingHandler.tracingMessageSender = mockTracingMessageSender;
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<AuditEventV2>(goodConfiguration, cb,  tracingHandler) {
            @Override
            protected Client buildClient(AuditConfiguration auditConfiguration, PublishConfiguration publishConfiguration)
                    throws EventHubClientException {
                auditCommonClientState = AuditCommonClientState.CONNECTING;
                return mockedClient;
            }
        };

        auditClientAsyncImpl.trace();

        verify(mockTracingMessageSender, times(1)).sendTracingMessage(any());
        verify(mockedClient, times(1)).addMessage(any(),any(),any());
    }


    @Test
    public void testAudit_auditClientIsDisconnected_shouldNotFlushData() throws AuditException, EventHubClientException {
        int morethanMaxRetry = 2500;
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);

        when(client.addMessage(any())).thenReturn(client);

        auditClientAsyncImpl.setStateAndNotify(AuditCommonClientState.DISCONNECTED);
        auditClientAsyncImpl.audit(auditEvent_1);
        try {
            Thread.sleep(morethanMaxRetry);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(client, never()).flush();
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(1));
    }

    @Test
    public void testAudit_auditClientIsConnected_shouldFlushData() throws AuditException, EventHubClientException {
        long morethanMaxRetry = 4000;
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);

        when(client.addMessage(any())).thenReturn(client);

        auditClientAsyncImpl.setStateAndNotify(AuditCommonClientState.CONNECTED);
        auditClientAsyncImpl.audit(auditEvent_1);
        try {
            Thread.sleep(morethanMaxRetry);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(client, atLeast(2)).flush();
        verify(client, atMost(3)).flush();
        assertThat(auditClientAsyncImpl.getEventMap().size(), is(1));
    }

    @Test
    public void shutdown_noEvents_clientIsShutdown() throws AuditException, EventHubClientException {
        TestHelper<AuditEventV2> cb = spy(new TestHelper<>());
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);

        auditClientAsyncImpl.shutdown();

        assertEquals(AuditClientState.SHUTDOWN, auditClientAsyncImpl.getAuditClientState());
        assertTrue(auditClientAsyncImpl.getRetryExecutorService().isShutdown());
        verifyZeroInteractions(cb);
    }

    @Test
    public void shutdown_eventMapNotEmpty_onFailureIsTriggered() throws AuditException, EventHubClientException {
        TestHelper<AuditEventV2> cb = spy(new TestHelper<>());
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        auditClientAsyncImpl.getEventMap().put(id_1, new EventContainer<>(auditEvent_1));

        auditClientAsyncImpl.shutdown();

        assertEquals(AuditClientState.SHUTDOWN, auditClientAsyncImpl.getAuditClientState());
        assertTrue(auditClientAsyncImpl.getRetryExecutorService().isShutdown());
        assertThat(cb.lastFailureCode, is(FailCode.NO_ACK));
        assertThat(cb.lastFailureEvent, is(auditEvent_1));
    }

    @Test
    public void graceful_noEvents_clientIsShutdown() throws AuditException, EventHubClientException {
        TestHelper<AuditEventV2> cb = spy(new TestHelper<>());
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);

        auditClientAsyncImpl.gracefulShutdown();

        assertEquals(AuditClientState.SHUTDOWN, auditClientAsyncImpl.getAuditClientState());
        assertTrue(auditClientAsyncImpl.getRetryExecutorService().isShutdown());
        verifyZeroInteractions(cb);
    }

    @Test
    public void graceful_eventMapNotEmpty_onFailureIsTriggered() throws AuditException, EventHubClientException {
        TestHelper<AuditEventV2> cb = spy(new TestHelper<>());
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        auditClientAsyncImpl.getEventMap().put(id_1, new EventContainer<>(auditEvent_1));
        long start = System.currentTimeMillis();

        auditClientAsyncImpl.gracefulShutdown();
        long end = System.currentTimeMillis();

        long diff = end-start;
        assertTrue(diff >= auditClientAsyncImpl.noAckLimit * (auditClientAsyncImpl.retryCount));
        assertTrue(diff < auditClientAsyncImpl.noAckLimit * (auditClientAsyncImpl.retryCount + 3));
        assertEquals(AuditClientState.SHUTDOWN, auditClientAsyncImpl.getAuditClientState());
        assertTrue(auditClientAsyncImpl.getRetryExecutorService().isShutdown());
        assertThat(cb.lastFailureCode, is(FailCode.NO_ACK));
        assertThat(cb.lastFailureEvent, is(auditEvent_1));
    }

    @Test
    public void graceful_eventBecomeEmpty_onFailureIsNotTriggered() throws AuditException, EventHubClientException {
        TestHelper<AuditEventV2> cb = spy(new TestHelper<>());
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        when(client.flush())
                .thenAnswer((c) -> {
                    auditClientAsyncImpl.handleEventHubCallback()
                            .onAck(Collections.singletonList(Ack.newBuilder()
                                    .setId(id_1)
                                    .setStatusCode(AckStatus.ACCEPTED)
                                    .build()));
                    return null;
                })
                .thenReturn(new ArrayList<>());
        auditClientAsyncImpl.getEventMap().put(id_1, new EventContainer<>(auditEvent_1));
        auditClientAsyncImpl.getEventQueue().add(new QueueElement(auditEvent_1.getMessageId(), auditEvent_1.getTimestamp()));

        long start = System.currentTimeMillis();
        auditClientAsyncImpl.gracefulShutdown();
        long end = System.currentTimeMillis();

        long diff = end-start;
        assertTrue(diff >= auditClientAsyncImpl.noAckLimit );
        assertTrue(diff < auditClientAsyncImpl.noAckLimit * (auditClientAsyncImpl.retryCount +1 ));
        assertEquals(AuditClientState.SHUTDOWN, auditClientAsyncImpl.getAuditClientState());
        assertTrue(auditClientAsyncImpl.getRetryExecutorService().isShutdown());
        assertThat(cb.lastSuccessEventsBatch.iterator().next().getMessageId(), is(auditEvent_1.getMessageId()));
        assertThat(cb.getSuccessCount(), is(1));
        assertTrue(auditClientAsyncImpl.getEventMap().isEmpty());
    }

    @Test
    public void reconnectStrategyTest() throws AuditException, EventHubClientException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        assertThat(auditClientAsyncImpl.getReconnectEngine(), is(instanceOf(ExponentialReconnectStrategy.class)));
    }

    @Test
    public void reconnectStrategyTest_reconnectIsNeededAndAttempted() throws AuditException, EventHubClientException, InterruptedException {
        reconnectAlgoTest(1000,10);
    }


    @Test
    public void reconnectStrategyTest_reconnectIsNeededAndAttempted2() throws AuditException, EventHubClientException, InterruptedException {
        reconnectAlgoTest(5,1000);
    }


    @Test
    public void reconnectStrategyTest_reconnectIsNeededAndAttempted3() throws AuditException, EventHubClientException, InterruptedException {
        reconnectAlgoTest(10000,1);
    }

    private void reconnectAlgoTest(int numOfMessage, int delayBetweenMessages) throws EventHubClientException, AuditException, InterruptedException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);

        when(client.addMessage(any())).thenReturn(client);
        when(client.flush()).thenAnswer((invocation) -> {
            auditClientAsyncImpl.handleEventHubCallback().onFailure(new Throwable(STREAM_CLOSED));
            return null;
        });
        long start = System.currentTimeMillis();
        for (int i = 0; i < numOfMessage; i++) {
            AuditEventV2 eventV2 = AuditEventV2.builder()
                    .timestamp(0)
                    .classifier(AuditEnums.Classifier.FAILURE)
                    .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                    .categoryType(AuditEnums.CategoryType.API_CALLS)
                    .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                    .payload("GET v2/apps T.O")
                    .build();
            auditClientAsyncImpl.audit(eventV2);
            Thread.sleep(delayBetweenMessages);
        }
        long timePassed = (System.currentTimeMillis()- start);
        int i,sum = 0;
        for(i = 0; i < ExponentialReconnectStrategy.reconnectIntervalsMillis.length; i++){
            if((ExponentialReconnectStrategy.reconnectIntervalsMillis[i] + sum) < timePassed){
                sum += ExponentialReconnectStrategy.reconnectIntervalsMillis[i];
            }
            else break;
        }
        log.info(String.format("reconnected %d times over %d seconds", i , timePassed/1000));
        //giving a bit of flexibility since we are dealing here with best effort timers and synchronized operations
        //generally speaking - we should expect i attempts.
        verify(client, atLeast(i-2)).reconnect();
        verify(client, atMost(i+2)).reconnect();
    }

    private AuditEventV2 cloneAndUpdateindexes(AuditEventV2 auditEvent, int i){
        return auditEvent.clone();
    }



    @Test
    public void validateEventsFailureTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        AuditEventV2 noClassifierEvent = mock(AuditEventV2.class);
        ReflectionUtils.modifyPrivateProperty(noClassifierEvent , "classifier" , null);

        auditClientAsyncImpl.audit(noClassifierEvent);

        assertThat(cb.getFailureCount(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.VALIDATION_ERROR));
    }

    @Test
    public void validateEventsFailureRealTest() throws EventHubClientException, AuditException {
        TestHelper<AuditEventV2> cb = new TestHelper<>();
        AuditClientAsyncImpl<AuditEventV2> auditClientAsyncImpl = new AuditClientAsyncImpl<>(goodConfiguration, cb, new TracingHandlerImpl(goodConfiguration,"ASYNC"));
        Client client = mock(Client.class);
        auditClientAsyncImpl.setClient(client);
        AuditEventV2 invalidTenantUuidEvent = mock(AuditEventV2.class);
        ReflectionUtils.modifyPrivateProperty(invalidTenantUuidEvent , "tenantUuid" , "a very verrrrrryyyyy looooooonngggg striiinnnnggg");

        auditClientAsyncImpl.audit(invalidTenantUuidEvent);

        assertThat(cb.getFailureCount(), is(1));
        assertThat(cb.lastFailureCode, is(FailCode.VALIDATION_ERROR));
    }
}