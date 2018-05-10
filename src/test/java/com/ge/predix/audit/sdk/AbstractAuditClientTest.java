package com.ge.predix.audit.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV1;
import com.ge.predix.audit.sdk.message.AuditTracingEvent;
import com.ge.predix.audit.sdk.message.tracing.Checkpoint;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSender;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSenderImpl;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.EventHubClientException;
import com.ge.predix.eventhub.client.Client;
import com.ge.predix.eventhub.configuration.PublishAsyncConfiguration;
import com.ge.predix.eventhub.configuration.PublishSyncConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by Martin Saad on 2/6/2017.
 */
public class AbstractAuditClientTest {

 /*   private static AuditConfiguration noBulkConfiguration;
    private static AuditConfiguration bulkConfiguration;
    private static AuditConfiguration goodConfiguration;
    private static AuditConfiguration wrongConfigurationWithoutBulk;
    private static ValidatorServiceImpl validatorService;
    private TracingMessageSender tracingMessageSender;
    private static ObjectMapper om;
    private static int queueSize = 20000;
    public static String eventhubZoneId = UUID.randomUUID().toString();

    @Before
    public void init() {
        tracingMessageSender = mock(TracingMessageSenderImpl.class);
        om = new ObjectMapper();
        validatorService = new ValidatorServiceImpl();

        noBulkConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .uaaUrl("http://localhost:443/uaa")
                .tracingInterval(1000*60)
                .tracingToken("token")
                .tracingUrl("http://localhost:443/tracing")
                .bulkMode(false)
                //.clientType(AuditClientType.ASYNC)
                .build();

        bulkConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .uaaUrl("http://localhost:443/uaa")
                .tracingInterval(300)
                .tracingToken("token")
                .tracingUrl("http://localhost:443/tracing")
                .bulkMode(true)
               // .clientType(AuditClientType.ASYNC)
                .build();

        goodConfiguration = AuditConfiguration.builder()
                .ehubHost("localhost/eh")
                .ehubPort(443)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .uaaClientSecret("secret")
                .tracingInterval(3000)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .uaaUrl("http://localhost:443/uaa")
                .bulkMode(false)
            //    .clientType(AuditClientType.ASYNC)
                .build();

        wrongConfigurationWithoutBulk = AuditConfiguration.builder()
                .ehubHost("")
                .ehubPort(0)
                .ehubZoneId("zoneId")
                .uaaClientId("uaa")
                .tracingInterval(300)
                .tracingUrl("http://localhost:443/tracing")
                .tracingToken("token")
                .uaaClientSecret("secret")
                .uaaUrl("")
                .bulkMode(false)
            //    .clientType(AuditClientType.ASYNC)
                .build();
    }

    @Test
    public void partitionOfGreenTest() throws EventHubClientException, AuditException {
        AuditClientAsync auditClient = new AuditClientAsync(noBulkConfiguration,new TestHelper(), tracingMessageSender);
        auditClient.setBulkSize(2);
        AuditEvent event1 = AuditEventV1.builder()
                .build();

        AuditEvent event2 = AuditEventV1.builder()
                .build();

        AuditEvent event3 = AuditEventV1.builder()
                .build();

        AuditEvent event4 = AuditEventV1.builder()
                .build();

        List<AuditEvent> eventList = Arrays.asList(event1,event2,event3,event4);

        auditClient.partitionOf(eventList,auditClient.getBulkSize()).forEach(auditEvents -> {
            assertThat(auditEvents.size(),is(2));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void partitionLengthIsZeroTest() throws EventHubClientException, AuditException {
        AuditClientAsync auditClient = new AuditClientAsync(noBulkConfiguration,new TestHelper(), tracingMessageSender);
        auditClient.setBulkSize(0);
        AuditEvent event1 = AuditEventV1.builder()
                .build();

        AuditEvent event2 = AuditEventV1.builder()
                .build();

        AuditEvent event3 = AuditEventV1.builder()
                .build();

        AuditEvent event4 = AuditEventV1.builder()
                .build();

        List<AuditEvent> eventList = Arrays.asList(event1,event2,event3,event4);
        auditClient.partitionOf(eventList,auditClient.getBulkSize()).forEach(auditEvents -> {
        });
    }

    @Test
    public void partitionSizeIsZeroTest() throws EventHubClientException, AuditException {
        AuditClientAsync auditClient = new AuditClientAsync(noBulkConfiguration, new TestHelper(), tracingMessageSender);
        auditClient.setBulkSize(2);
        List<AuditEvent> eventList = new ArrayList<>();
        auditClient.partitionOf(eventList,auditClient.getBulkSize()).forEach(auditEvents -> {
            assertThat(auditEvents.isEmpty(),is(true));
        });
    }

    @Test
    public void validateEventsFailureTest() throws EventHubClientException, AuditException {
        TestHelper callback = new TestHelper();
        AuditClientAsync auditClient = new AuditClientAsync(bulkConfiguration, callback, tracingMessageSender);
        auditClient.setValidatorService(validatorService);
        AuditEventV1 wrongVersionEvent = mock(AuditEventV1.class);
        when(wrongVersionEvent.getVersion()).thenReturn(-1);
        auditClient.sanitizeEvents(Arrays.asList(wrongVersionEvent));
        int validate_failure_success = callback.getValidateCount();
        assertThat(validate_failure_success,is(0));
    }

    @Test
    public void validateEventsEmptyReportTest() throws EventHubClientException, AuditException {
        TestHelper callback = new TestHelper();
        AuditClientAsync auditClient = new AuditClientAsync(bulkConfiguration, callback, tracingMessageSender);
        auditClient.setValidatorService(validatorService);
        AuditEventV1 wrongVersionEvent = new AuditEventV1();
        auditClient.sanitizeEvents(Arrays.asList(wrongVersionEvent));
        int validate_failure_success = callback.getValidateCount();
        assertThat(validate_failure_success,is(0));
    }

    @Test
    public void validateEventReportTest() throws EventHubClientException, AuditException {
        TestHelper callback = new TestHelper();
        AuditClientAsync auditClient = new AuditClientAsync(bulkConfiguration, callback, tracingMessageSender);
        auditClient.setValidatorService(validatorService);
        AuditEventV1 fishyEvent = new AuditEventV1();
        String fishyDescription = "<img src=>";
        fishyEvent.setDescription(fishyDescription.toString());
        auditClient.sanitizeEvents(Arrays.asList(fishyEvent));
        int validate_failure_success = callback.getValidateCount();
        assertTrue(validate_failure_success ==1 &&
                !fishyEvent.getDescription().equals(fishyDescription)
        );
    }

    @Test
    public void addToEventhubCacheEmptyListTest() throws EventHubClientException, JsonProcessingException, AuditException {
        List<AuditEvent> events = new ArrayList<>();
        AuditClientAsync auditClient = new AuditClientAsync(goodConfiguration, new TestHelper(), tracingMessageSender);
        auditClient.setQueueSize(queueSize);
        auditClient.addToEventhubCache(events);
    }


    @Test
    public void addToEventhubCacheJsonProcessFailureTest() throws EventHubClientException, JsonProcessingException, AuditException {
        ObjectMapper mockedObjectMapper = mock(ObjectMapper.class);
        when(mockedObjectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

        List<AuditEvent> events = Arrays.asList(new AuditEventV1());
        TestHelper cb = new TestHelper();
        AuditClientAsync auditClient = new AuditClientAsync(goodConfiguration, cb, tracingMessageSender);
        auditClient.setOm(mockedObjectMapper);
        auditClient.addToEventhubCache(events);
        assertThat(cb.getFailures(),is(1));
    }

    @Test
    public void addToEventhubCacheAddMessageFailureTest() throws EventHubClientException, JsonProcessingException, AuditException {
        Client mockedClient = mock(Client.class);
        when(mockedClient.addMessage(any(),anyString(),any())).thenThrow(EventHubClientException.AddMessageException.class);
        TestHelper cb = new TestHelper();
        AuditClientAsync auditClient = spy(new AuditClientAsync(goodConfiguration, cb, tracingMessageSender));
        auditClient.setClient(mockedClient);
        auditClient.setOm(om);
        List<AuditEvent> events = Arrays.asList(new AuditEventV1());
        auditClient.addToEventhubCache(events);
        assertThat(cb.getFailures(),is(1));
    }

    @Test
    public void addToEventhubCacheTest() throws EventHubClientException, JsonProcessingException, AuditException {
        Client mockedClient = mock(Client.class);
        Client mocked2Client = mock(Client.class);
        when(mockedClient.addMessage(any())).thenReturn(mocked2Client);

        AuditClientAsync auditClient = new AuditClientAsync(goodConfiguration, new TestHelper(), tracingMessageSender);
        auditClient.setOm(om);
        spy(auditClient);
        auditClient.setClient(mockedClient);

        List<AuditEvent> events = Arrays.asList(new AuditEventV1());
        auditClient.addToEventhubCache(events);
    }

    @Test(expected = EventHubClientException.class)
    public void buildClientFailTest() throws EventHubClientException, AuditException {
        AuditClientAsync auditClientSync = new AuditClientAsync(wrongConfigurationWithoutBulk, new TestHelper(), tracingMessageSender);
        PublishAsyncConfiguration publishConfiguration = new PublishAsyncConfiguration.Builder().build();
        auditClientSync.buildClient(wrongConfigurationWithoutBulk, publishConfiguration);
        auditClientSync.setAutomaticTokenRenew(true);
    }

    @Test
    public void buildClientAsyncConfigurationTest() throws EventHubClientException, AuditException {
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(), tracingMessageSender);
        PublishAsyncConfiguration publishConfiguration = new PublishAsyncConfiguration.Builder().build();
        Client client = auditClientSync.buildClient(goodConfiguration, publishConfiguration);
        assertNotNull(client);
    }

    @Test
    public void buildClientSyncConfigurationTest() throws EventHubClientException, AuditException {
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(), tracingMessageSender);
        PublishSyncConfiguration publishConfiguration = new PublishSyncConfiguration.Builder()
                .timeout(1000).build();
        Client client = auditClientSync.buildClient(goodConfiguration, publishConfiguration);
        assertNotNull(client);
    }

    @Test
    public void sendTracingMessageTest() throws EventHubClientException, InterruptedException {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(), new TracingMessageSender() {

            @Override
            public void sendTracingMessage(Checkpoint checkPoint) {
                atomicInteger.incrementAndGet();
            }
        });

        Thread.sleep(5500);
        assertThat(auditClientSync.getAuditTracingEvent().get().getEventhubZoneId(),
                is(goodConfiguration.getEhubZoneId()));
        assertThat(atomicInteger.get(),is(1));
    }


    @Test
    public void sendTracingMessageFailurAndContinueTest() throws EventHubClientException, InterruptedException {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(),
                checkPoint -> atomicInteger.incrementAndGet());
        Thread.sleep(goodConfiguration.getTracingInterval()+goodConfiguration.getTracingInterval()/2);

        assertThat(atomicInteger.get()>=1,is(true));
        assertThat(atomicInteger.get()<3,is(true));
    }

    @Test
    public void checkIsTracingAckDifferentIdTest() throws EventHubClientException {
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(), tracingMessageSender);
        UUID uuid = UUID.randomUUID();
        auditClientSync.getAuditTracingEvent().set(new AuditTracingEvent(uuid.toString(),0,eventhubZoneId));

        Ack ack = Ack.newBuilder().setId("1").build();
        boolean result = auditClientSync.sendCheckpoint(ack);

        assertThat(result, is(false));
    }

    @Test
    public void checkIsTracingAckSameIdTest() throws EventHubClientException{
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(), tracingMessageSender);
        UUID uuid = UUID.randomUUID();
        auditClientSync.getAuditTracingEvent().set(new AuditTracingEvent(uuid.toString(),0,eventhubZoneId));

        Ack ack = Ack.newBuilder().setId(uuid.toString()).build();
        boolean result = auditClientSync.sendCheckpoint(ack);

        assertThat(result, is(true));
    }

    @Test
    public void checkIsTracingAckSameIdWithExceptionTest() throws EventHubClientException{
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration, new TestHelper(), new TracingMessageSender() {

            @Override
            public void sendTracingMessage(Checkpoint checkPoint) {
            }
        });
        UUID uuid = UUID.randomUUID();
        auditClientSync.getAuditTracingEvent().set(new AuditTracingEvent(uuid.toString(),0,eventhubZoneId));

        Ack ack = Ack.newBuilder().setId(uuid.toString()).build();
        boolean result = auditClientSync.sendCheckpoint(ack);

        assertThat(result, is(true));
    }


    @Test
    public void checkIsTracingEventTest() throws EventHubClientException {
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration,
                new TestHelper(), tracingMessageSender);

        UUID uuid = UUID.randomUUID();
        auditClientSync.getAuditTracingEvent().set(new AuditTracingEvent(uuid.toString(),0,eventhubZoneId));
        AuditEvent auditEvent = auditClientSync.getAuditTracingEvent().get();


        assertThat(auditClientSync.checkIsTracingEvent(auditEvent), is(true));
    }

    @Test
    public void checkIsTracingEventAndNotifyOnFailureTest() throws EventHubClientException, IOException {
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration,
                new TestHelper(), tracingMessageSender);

        UUID uuid = UUID.randomUUID();
        auditClientSync.getAuditTracingEvent().set(new AuditTracingEvent(uuid.toString(),0,eventhubZoneId));
        AuditEvent auditEvent = auditClientSync.getAuditTracingEvent().get();

        auditClientSync.sendCheckpointFail(auditEvent, "kuku");

        verify(tracingMessageSender).sendTracingMessage(any());
    }

    @Test
    public void checkIsTracingEventAndNotifyOnFailureWithExceptionTest()
            throws EventHubClientException, IOException {
        AuditClientAsync auditClientSync = new AuditClientAsync(goodConfiguration,
                new TestHelper(), new TracingMessageSender() {
            @Override
            public void sendTracingMessage(Checkpoint checkPoint) {
            }
        });

        UUID uuid = UUID.randomUUID();
        auditClientSync.getAuditTracingEvent().set(new AuditTracingEvent(uuid.toString(),0,eventhubZoneId));
        AuditEvent auditEvent = auditClientSync.getAuditTracingEvent().get();
        boolean result = auditClientSync.sendCheckpointFail(auditEvent, "kuku");

        assertThat(result, is(true));
    }*/
}
