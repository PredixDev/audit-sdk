package com.ge.predix.audit.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.*;

import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.util.ReflectionUtils;

import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.AckStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.client.Client;

@SuppressWarnings("unchecked")
public class AuditClientSyncImplTest {

    //TODO test shutdown
    public static String eventhubZoneId = UUID.randomUUID().toString();
    private TracingHandler tracingHandler;
    private static AuditConfiguration bulkConfiguration;
    private static AuditConfiguration badConfiguration;
    private static ObjectMapper om;
    private int defaultRetryCount = 2;
    
    @Before
    public void init() {
        tracingHandler = Mockito.mock(TracingHandler.class);
        om = new ObjectMapper();

        badConfiguration = AuditConfiguration.builder()
                .ehubHost("")
                .ehubPort(0)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .tracingInterval(1000*60*10)
                .tracingInterval(1000*60*10)
                .tracingUrl("http://localhost:443/tracing")
                .uaaClientSecret("secret")
                .uaaUrl("")
                .bulkMode(false)
                .build();

        bulkConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .tracingInterval(1000*60*10)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(true)
                .maxRetryCount(defaultRetryCount)
                .retryIntervalMillis(200)
                .build();
    }
    
    @Test(expected = EventHubClientException.class)
    public void initFailureTest() throws EventHubClientException, AuditException {
    	new AuditClientSyncImpl(badConfiguration, tracingHandler);
    }

    @Test
    public void validateEventsFailureTest() throws EventHubClientException, AuditException {
        AuditClientSyncImpl<AuditEvent> auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        Client client = mock(Client.class);
        auditClientSync.setClient(client);
        AuditEventV2 noClassifierEvent = mock(AuditEventV2.class);
        ReflectionUtils.modifyPrivateProperty(noClassifierEvent , "classifier" , null);

        AuditingResult<AuditEvent> result= auditClientSync.audit(noClassifierEvent);

        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.VALIDATION_ERROR, result.getFailedEvents().iterator().next().getFailureReason());
        verify(client, never()).addMessage(any(), any(), any());
    }

    @Test
    public void validateEventsFailureRealTest() throws EventHubClientException, AuditException {
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        Client client = mock(Client.class);
        auditClientSync.setClient(client);
        AuditEventV2 invalidTenantUuidEvent = mock(AuditEventV2.class);
        ReflectionUtils.modifyPrivateProperty(invalidTenantUuidEvent , "tenantUuid" , "a very verrrrrryyyyy looooooonngggg striiinnnnggg");

        AuditingResult<AuditEvent> result = auditClientSync.audit(invalidTenantUuidEvent);

        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.VALIDATION_ERROR, result.getFailedEvents().get(0).getFailureReason());
        verify(client, never()).addMessage(any(), any(), any());
    }

    @Test
    public void addToEventhubCacheJsonProcessFailureTest() throws EventHubClientException, JsonProcessingException, AuditException {
        ObjectMapper mockedObjectMapper = mock(ObjectMapper.class);
        when(mockedObjectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        List<AuditEvent> events = Collections.singletonList(getAuditEvent());
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setOm(mockedObjectMapper);
        Client client = mock(Client.class);
        auditClientSync.setClient(client);
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.JSON_ERROR, result.getFailedEvents().get(0).getFailureReason());
        verify(client, never()).addMessage(any(), any(), any());
    }

    @Test
    public void addToEventhubCacheAddMessageFailureTest() throws EventHubClientException, JsonProcessingException, AuditException {
        Client mockedClient = mock(Client.class);
        when(mockedClient.addMessage(any(),anyString(),any())).thenThrow(EventHubClientException.AddMessageException.class);
        AuditClientSyncImpl auditClientSync = spy(new AuditClientSyncImpl(bulkConfiguration, tracingHandler));
        auditClientSync.setClient(mockedClient);
        auditClientSync.setOm(om);
        List<AuditEvent> events = Collections.singletonList(getAuditEvent());

        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.ADD_MESSAGE_ERROR, result.getFailedEvents().get(0).getFailureReason());
    }

    @Test
    public void flushWithRetriesEmptyTest() throws EventHubClientException, AuditException {
        Client mockedClient = mock(Client.class);
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        
        auditClientSync.audit(new ArrayList<>());
        
        verify(mockedClient, never()).addMessage(any(), any(), any());
        verify(mockedClient, never()).flush();
    }
    
	@Test
    public void flushEventhubFails() throws EventHubClientException, AuditException {
    	Client mockedClient = mock(Client.class);
    	ReconnectStrategy mockReconnect = mock(ReconnectStrategy.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenThrow(EventHubClientException.class);
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setReconnectEngine(mockReconnect);
        auditClientSync.setRetryCount(0);
        List<AuditEvent> events = Collections.singletonList(getAuditEvent());
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(any(), any(), any());
        verify(mockReconnect, times(1)).notifyStateChanged(any());
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.NO_ACK, result.getFailedEvents().get(0).getFailureReason());
    }
    
    @Test
    public void flushEventhubReturnsEmptyAcksList() throws EventHubClientException, AuditException, InterruptedException {
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(new ArrayList<>());
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setRetryCount(0);
        List<AuditEvent> events = Collections.singletonList(getAuditEvent());
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(any(), any(), any());
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.NO_ACK, result.getFailedEvents().get(0).getFailureReason());
    }
    
    @Test
    public void flushEventhubReturnsPartialAcksList() throws EventHubClientException, AuditException {
    	AuditEvent successEvent = getAuditEvent();
    	AuditEvent noAckEvent = getAuditEvent();
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(Arrays.asList(
    			Ack.newBuilder().setStatusCode(AckStatus.ACCEPTED).setId(successEvent.getMessageId()).build()
    	));
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setRetryCount(0);
        List<AuditEvent> events = Arrays.asList(successEvent, noAckEvent);
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        verify(mockedClient, times(2)).addMessage(any(), any(), any());
        assertEquals(1, result.getSentEvents().size());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.NO_ACK, result.getFailedEvents().get(0).getFailureReason());
    }
    
    @Test
    public void handleAcceptedAcksTest() throws EventHubClientException, AuditException {

        AuditEvent auditEvent = getAuditEvent();
        String msgId = auditEvent.getMessageId();
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(Arrays.asList(Ack.newBuilder().setStatusCode(AckStatus.ACCEPTED).setId(msgId).build()));
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);

        List<AuditEvent> events = Collections.singletonList(auditEvent);
        
        AuditingResult result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(any(), any(), any());
        verify(mockedClient, times(1)).flush();
        assertTrue(result.getFailedEvents().isEmpty());
        assertEquals(1, result.getSentEvents().size());
        assertEquals(events.get(0), result.getSentEvents().get(0));
    }
    
    @Test
    public void handleAcceptedAcksTest_eventNotInMap() throws EventHubClientException, AuditException {
    	String msgId = UUID.randomUUID().toString();
    	String msgId2 = UUID.randomUUID().toString();
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(Arrays.asList(Ack.newBuilder().setStatusCode(AckStatus.ACCEPTED).setId(msgId2).build()));
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setRetryCount(0);
        List<AuditEvent> events = Collections.singletonList(getAuditEvent());
        
        AuditingResult result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(any(), any(), any());
        verify(mockedClient, times(1)).flush();
        assertTrue(result.getFailedEvents().isEmpty());
        assertTrue(result.getSentEvents().isEmpty());
    }
    
    @Test
    public void handleNotAcceptedAcksTest() throws EventHubClientException, AuditException {
        AuditEvent auditEvent = getAuditEvent();
        String msgId = auditEvent.getMessageId();
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(Arrays.asList(Ack.newBuilder().setStatusCode(AckStatus.BAD_REQUEST).setId(msgId).build()));
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setRetryCount(0);
        List<AuditEvent> events = Collections.singletonList(auditEvent);
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(any(), any(), any());
        verify(mockedClient, times(1)).flush();
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.BAD_ACK, result.getFailedEvents().get(0).getFailureReason());
    }
    
    @Test
    public void handleNotAcceptedAcksTest_eventNotInMap() throws EventHubClientException, AuditException {
    	String msgId = UUID.randomUUID().toString();
    	String msgId2 = UUID.randomUUID().toString();
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(Arrays.asList(Ack.newBuilder().setStatusCode(AckStatus.BAD_REQUEST).setId(msgId2).build()));
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setRetryCount(0);
        List<AuditEvent> events = Collections.singletonList(getAuditEvent());
        
        AuditingResult result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(any(), any(), any());
        verify(mockedClient, times(1)).flush();
        assertTrue(result.getFailedEvents().isEmpty());
        assertTrue(result.getSentEvents().isEmpty());
    }
    
    @Test
    public void auditRetryTest() throws AuditException, EventHubClientException {
    	int retryCount = 8;
    	Client mockedClient = mock(Client.class);
    	AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setOm(om);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setBulkSize(1);
        auditClientSync.setRetryCount(retryCount);
        AuditEvent event2 = getAuditEvent();
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(event2);
        
		verify(mockedClient, times(retryCount + 1)).addMessage(any(), any(), any());
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(1, result.getFailedEvents().size());
        assertEquals(FailCode.NO_ACK, result.getFailedEvents().get(0).getFailureReason());
    }
    
    @Test
    public void auditDefaultRetryCountTest() throws AuditException, EventHubClientException {
    	AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
    	Client mockedClient = mock(Client.class);
        auditClientSync.setOm(om);
        auditClientSync.setClient(mockedClient);
        AuditEvent event2 = getAuditEvent();
        auditClientSync.setBulkSize(1);
        
        auditClientSync.audit(event2);
        
        verify(mockedClient, times(defaultRetryCount + 1)).addMessage(any(), any(), any());
    }
    
    @Test
    public void auditNoRetryAfterDisconnect() throws AuditException, EventHubClientException {
    	AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenThrow(EventHubClientException.class); // will change state to DISCONNECTED
        auditClientSync.setClient(mockedClient);
        auditClientSync.setReconnectEngine(mock(ReconnectStrategy.class));
        List<AuditEvent> events = Arrays.asList(
                getAuditEvent(),
                getAuditEvent()
        );
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        verify(mockedClient, times(events.size() * 2)).addMessage(any(), any(), any());
        verify(mockedClient, times(1)).flush();
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(2, result.getFailedEvents().size());
        assertEquals(FailCode.NO_ACK, result.getFailedEvents().get(0).getFailureReason());
        assertEquals(FailCode.NO_ACK, result.getFailedEvents().get(1).getFailureReason());
    }
    
    @Test
    public void multipleEventsFullFlowTest() throws Exception {
    	AuditEvent successEvent = getAuditEvent();
    	AuditEvent failedEvent = getAuditEvent();
    	AuditEvent noAckEvent = getAuditEvent();
    	Client mockedClient = mock(Client.class);
    	when(mockedClient.addMessage(any())).thenReturn(mockedClient);
    	when(mockedClient.flush()).thenReturn(Arrays.asList(
    			Ack.newBuilder().setStatusCode(AckStatus.ACCEPTED).setId(successEvent.getMessageId()).build(),
    			Ack.newBuilder().setStatusCode(AckStatus.FAILED).setId(failedEvent.getMessageId()).build()
    	));
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        List<AuditEvent> events = Arrays.asList(successEvent, failedEvent, noAckEvent);
        
        AuditingResult<AuditEvent> result = auditClientSync.audit(events);
        
        verify(mockedClient, times(1)).addMessage(eq(successEvent.getMessageId()), any(), any());
        verify(mockedClient, times(defaultRetryCount + 1)).addMessage(eq(failedEvent.getMessageId()), any(), any());
        verify(mockedClient, times(defaultRetryCount + 1)).addMessage(eq(noAckEvent.getMessageId()), any(), any());
        assertEquals(1, result.getSentEvents().size());
        assertEquals(successEvent, result.getSentEvents().get(0));
        assertEquals(2, result.getFailedEvents().size());
        AuditEventFailReport failed = result.getFailedEvents().stream().filter(e -> e.getAuditEvent().equals(failedEvent)).findFirst().get();
        AuditEventFailReport noAck = result.getFailedEvents().stream().filter(e -> e.getAuditEvent().equals(noAckEvent)).findFirst().get();
        assertNotNull(failed);
        assertNotNull(noAck);
        assertEquals(FailCode.BAD_ACK, failed.getFailureReason());
        assertEquals(FailCode.NO_ACK, noAck.getFailureReason());
    }

    @Test
    public void auditClientFullFlowSuccessHeavyTest() throws EventHubClientException, AuditException {
        int bulkSize = 100;
    	Client mockedClient = mock(Client.class);
        int numOfEvents = 2000;
        List<AuditEvent> successEvents = new ArrayList<>(numOfEvents);
        List<Ack> ackList = new ArrayList<>(numOfEvents);
        for (int i = 0; i < numOfEvents; i++) {
            AuditEvent event = getAuditEvent();
            successEvents.add(event);
            ackList.add(Ack.newBuilder().setId(event.getMessageId()).setStatusCode(AckStatus.ACCEPTED).build());
        }
        when(mockedClient.flush()).thenReturn(ackList);
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setBulkSize(bulkSize);
        
        AuditingResult result = auditClientSync.audit(successEvents);
        
        verify(mockedClient, times(numOfEvents)).addMessage(any(), any(), any());
        verify(mockedClient, times(numOfEvents / bulkSize)).flush();
        assertTrue(result.getFailedEvents().isEmpty());
        assertEquals(numOfEvents, result.getSentEvents().size());
    }

    @Test
    public void auditClientFullFlowFailureHeavyTest() throws EventHubClientException, AuditException {
    	int bulkSize = 100;
    	Client mockedClient = mock(Client.class);
        int numOfEvents = 500;
        List<AuditEvent> successEvents = new ArrayList<>(numOfEvents);
        List<Ack> ackList = new ArrayList<>(numOfEvents);
        for (int i = 0; i < numOfEvents; i++) {
            AuditEvent event = getAuditEvent();
            successEvents.add(event);
            ackList.add(Ack.newBuilder().setId(event.getMessageId()).setStatusCode(AckStatus.FAILED).build());
        }
        when(mockedClient.flush()).thenReturn(ackList);
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        auditClientSync.setClient(mockedClient);
        auditClientSync.setBulkSize(bulkSize);
        
        AuditingResult result = auditClientSync.audit(successEvents);
        
        verify(mockedClient, times(numOfEvents * (defaultRetryCount + 1))).addMessage(any(), any(), any());
        verify(mockedClient, times((numOfEvents / bulkSize) * (defaultRetryCount + 1))).flush();
        assertTrue(result.getSentEvents().isEmpty());
        assertEquals(numOfEvents, result.getFailedEvents().size());
    }
    
    @Test
    public void reconnectTest() throws EventHubClientException {
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        Client client = mock(Client.class);
        auditClientSync.setClient(client);
        
        auditClientSync.reconnect();

        verify(client).reconnect();
    }
    
    @Test
    public void shutdownTest() throws EventHubClientException {
        AuditClientSyncImpl auditClientSync = new AuditClientSyncImpl(bulkConfiguration, tracingHandler);
        Client client = mock(Client.class);
        auditClientSync.setClient(client);
        
        auditClientSync.shutdown();

        verify(client).shutdown();
    }

    private AuditEvent getAuditEvent(){
        return AuditEventV2.builder()
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.STARTUP_EVENT)
                .classifier(AuditEnums.Classifier.SUCCESS)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .build();
    }
}
