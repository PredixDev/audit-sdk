package com.ge.predix.audit.sdk.routing;

import com.ge.predix.audit.sdk.AuditClientState;
import com.ge.predix.audit.sdk.config.*;
import com.ge.predix.audit.sdk.exception.RoutingAuditException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.google.common.collect.Queues;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class RoutingAuditClientTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    private RoutingTestHelper<AuditEventV2> cb = new RoutingTestHelper<>();
    private RoutingAuditPublisher<AuditEventV2> publisher = mock(RoutingAuditPublisher.class);
    private AtomicReference<AuditClientState> atomicReference = spy(new AtomicReference<>(AuditClientState.CONNECTED));
    private LinkedBlockingQueue<List<AuditEventV2>> auditEventV2sQ = spy(Queues.newLinkedBlockingQueue(2));
    private RoutingAuditClient<AuditEventV2> routingAuditClient = new RoutingAuditClient<AuditEventV2>(
            auditEventV2sQ,
            executorService,
            publisher,
            atomicReference,
            cb);


    @Test
    public void auditNullTriggersNothing(){
        AuditEventV2 eventV2 = null;
        routingAuditClient.audit(eventV2);

        verifyZeroInteractions(publisher);
    }

    @Test
    public void auditEmptyListTriggersNothing(){
        routingAuditClient.audit(new ArrayList<>());

        verifyZeroInteractions(publisher);
    }

    @Test
    public void auditNullListTriggersNothing(){
        List<AuditEventV2> eventV2List = null;
        routingAuditClient.audit(eventV2List);

        verifyZeroInteractions(publisher);
    }

    @Test
    public void sharedAuditEventTriggersPublisher() throws InterruptedException {
        AuditEventV2 v2 = AuditEventV2.builder().publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT).build();
        routingAuditClient.audit(v2);

        sleep(500);
        verify(publisher).auditToShared(Collections.singletonList(v2));
    }

    @Test
    public void dedicatedAuditEventsTriggersPublisherInTheReightOrder() throws InterruptedException {
        AuditEventV2 v2First = AuditEventV2.builder().publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .tenantUuid("1")
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT).build();

        AuditEventV2 v2Second = AuditEventV2.builder().publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .tenantUuid("2")
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT).build();

        AuditEventV2 v2Third = AuditEventV2.builder().publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .tenantUuid(v2First.getTenantUuid())
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT).build();

        routingAuditClient.audit(Arrays.asList(v2First, v2Second, v2Third, null));

        sleep(500);
        verify(publisher).auditToTenant(v2First.getTenantUuid(), Arrays.asList(v2First, v2Third));
        verify(publisher).auditToTenant(v2Second.getTenantUuid(), Collections.singletonList(v2Second));
    }

    @Test
    public void auditTooManyEventsCauseException() throws InterruptedException {
        BlockingQueue<List<AuditEventV2>> blockingQueue = mock(BlockingQueue.class);

        routingAuditClient = new RoutingAuditClient<AuditEventV2>(
                blockingQueue,
                executorService,
                publisher,
                atomicReference,
                cb);

        when(blockingQueue.offer(any())).thenReturn(false);

        assertThatThrownBy( ()-> routingAuditClient.audit(AuditEventV2.builder().publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .tenantUuid("1")
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT).build())).isInstanceOf(RoutingAuditException.class);

    }


    @Test
    public void shouldNotLetAuditWhenStatusIsShutdown() {
        routingAuditClient = new RoutingAuditClient<>(Queues.newLinkedBlockingQueue(2),
                Executors.newFixedThreadPool(1), publisher, new AtomicReference<>(AuditClientState.SHUTDOWN), cb);

        assertThatThrownBy(() -> routingAuditClient.audit(AuditEventV2.builder().publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT).build())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shutdown() {
        routingAuditClient.shutdown();

        assertTrue(executorService.isShutdown());
        verify(publisher).shutdown();
    }

    @Test
    public void gracefulShutdown() throws Exception {
        routingAuditClient.gracefulShutdown();

        assertTrue(executorService.isShutdown());
        verify(publisher).gracefulShutdown();
    }

    @Test(expected = RoutingAuditException.class)
    public void ShouldNotBuildRoutingClientWhenAppNameIsNotAvailable() {
        routingAuditClient = new RoutingAuditClient<>(RoutingAuditConfiguration.builder()
                .appNameConfig(AppNameConfig.builder()
                        .clientSecret("a")
                        .clientId("a")
                        .uaaUrl("a")
                        .build())
                .sharedAuditConfig(SharedAuditConfig.builder()
                        .auditZoneId("b")
                        .ehubUrl("b")
                        .ehubZoneId("b")
                        .uaaClientId("b")
                        .uaaClientSecret("b")
                        .uaaUrl("b")
                        .tracingToken("b")
                        .tracingUrl("b")
                        .build())
                .systemConfig(SystemConfig.builder()
                        .clientSecret("c")
                        .clientId("c")
                        .tokenServiceUrl("url")
                        .tmsUrl("url")
                        .build())
                .tenantAuditConfig(TenantAuditConfig.builder()
                        .auditServiceName("a")
                        .spaceName("c")
                        .build())
                .build(), cb);
    }

}