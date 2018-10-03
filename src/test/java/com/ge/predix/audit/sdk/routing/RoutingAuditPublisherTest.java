package com.ge.predix.audit.sdk.routing;

import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.TmsClientException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventExtended;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.message.AuditEventsConverter;
import com.ge.predix.audit.sdk.routing.cache.TenantCacheProxy;
import com.ge.predix.audit.sdk.routing.tms.AppNameClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RoutingAuditPublisherTest {

    private static final String APP_NAME = "kuku";
    private static final String TENANT = "tnt";
    private RoutingTestHelper<AuditEventV2>  callback = new RoutingTestHelper<>();
    private AppNameClient appNameClient = mock(AppNameClient.class);
    private TenantCacheProxy proxy = mock(TenantCacheProxy.class);
    private AuditEventsConverter converter = new AuditEventsConverter(appNameClient);
    private CommonClientInterface commonClientInterface = mock(CommonClientInterface.class);
    private RoutingAuditPublisher<AuditEventV2> auditEventV2RoutingAuditPublisher = new RoutingAuditPublisher<>(callback, proxy, converter);

    @Before
    public void init () {
        when(appNameClient.getAppName()).thenReturn(APP_NAME);
    }

    @Test
    public void auditPerTenantTenantIsSharedSharedClientIsUsed() throws AuditException {
        when(proxy.getClientFor(eq(TENANT))).thenThrow(new RuntimeException("Cannot get clients!"));
        when(proxy.getDefaultClient()).thenReturn(commonClientInterface);
        AuditEventV2 eventV2 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .build();

        auditEventV2RoutingAuditPublisher.auditToShared(Collections.singletonList(eventV2));


        AuditEventExtended extended = converter.extend(eventV2);
        verify(commonClientInterface).audit(Collections.singletonList(extended));
    }

    @Test
    public void auditPerTenantTenantIsDedicatedDedicatedClientIsUsed() throws AuditException {
        when(proxy.getDefaultClient()).thenThrow(new RuntimeException("Cannot get clients!"));
        when(proxy.getClientFor(eq(TENANT))).thenReturn(commonClientInterface);


        AuditEventV2 eventV2 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .tenantUuid(TENANT)
                .build();

        auditEventV2RoutingAuditPublisher.auditToTenant(TENANT, Collections.singletonList(eventV2));


        AuditEventExtended extended = converter.extend(eventV2);
        verify(commonClientInterface).audit(Collections.singletonList(extended));
    }

    @Test
    public void auditPerTenantTenantIsSharedThrowsExceptionIsSwallowedAndCallbackIsTriggered() {
        when(proxy.getDefaultClient()).thenThrow(new TmsClientException("Could not get audit instance"));
        AuditEventV2 v2 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .build();

        auditEventV2RoutingAuditPublisher.auditToShared(Collections.singletonList(v2));

        verifyZeroInteractions(commonClientInterface);
        assertEquals(FailReport.CLIENT_INITIALIZATION_ERROR, callback.getKpis(v2.getTenantUuid()).getLastFailureCode());
    }

    @Test
    public void auditPerTenantTenantIsDedicatedThrowsExceptionExceptionIsSwallowed() {
        when(proxy.getClientFor(TENANT)).thenThrow(new TmsClientException("Could not get audit instance"));

        auditEventV2RoutingAuditPublisher.auditToTenant(TENANT, Collections.singletonList(AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                .eventType(AuditEnums.EventType.SHUTDOWN_EVENT)
                .tenantUuid(TENANT)
                .build()));

        verifyZeroInteractions(commonClientInterface);
        assertEquals(FailReport.CLIENT_INITIALIZATION_ERROR, callback.getKpis(TENANT).getLastFailureCode());
    }

    @Test
    public void shutdown() {
        auditEventV2RoutingAuditPublisher.shutdown();

        verify(proxy).shutDown();
    }

    @Test
    public void gracefulShutdown() throws Exception {
        auditEventV2RoutingAuditPublisher.gracefulShutdown();

        verify(proxy).gracefulShutdown();
    }
}