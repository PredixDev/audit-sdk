package com.ge.predix.audit.sdk.routing;

import com.ge.predix.audit.sdk.AuditEventFailReport;
import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.FailCode;
import com.ge.predix.audit.sdk.Result;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventsConverter;
import com.ge.predix.audit.sdk.routing.cache.TenantCacheProxy;
import com.ge.predix.audit.sdk.util.ExceptionUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RoutingAuditPublisher<T extends AuditEvent> {

    private final RoutingAuditCallback<T> callback;
    private final TenantCacheProxy tenantCacheProxy;
    private final AuditEventsConverter converter;

    public void auditToTenant(String tenantUuid, List<T> events) {
        publish( ()-> tenantCacheProxy.getClientFor(tenantUuid), events,
                String.format("Failed to audit events to Audit instance of tenantUuid %s", tenantUuid));
    }

    public void auditToShared(List<T> events)  {
        publish(tenantCacheProxy::getDefaultClient, events,
                "Failed to audit events to application shared Audit instance");
    }

    private synchronized void publish(Supplier<CommonClientInterface<T>> supplier, List<T> events, String error)  {
        try {
            @SuppressWarnings("unchecked") List<T> eventsToSend = (List<T>) events.stream().map(converter::extend).collect(Collectors.toList());
            supplier.get().audit(eventsToSend);
        } catch (Exception e) {
            List<AuditEventFailReport<T>> failReports = new ArrayList<>();
            events.forEach(event ->
                    failReports.add(AuditEventFailReport.<T>builder()
                            .auditEvent(event)
                            .failureReason(FailCode.CLIENT_INITIALIZATION_ERROR)
                            .description(error)
                            .throwable(e)
                            .build()));
            if(!failReports.isEmpty()) {
                callback.onFailure(Result.<T>builder().failReports(failReports).build());
            }
        }
    }

    public void shutdown() {
        tenantCacheProxy.shutDown();
    }

    public void gracefulShutdown() throws Exception {
        tenantCacheProxy.gracefulShutdown();
    }

}
