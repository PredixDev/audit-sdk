package com.ge.predix.audit.sdk.routing;

import com.ge.predix.audit.sdk.CommonClientInterface;
import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventsConverter;
import com.ge.predix.audit.sdk.routing.cache.TenantCacheProxy;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.ExceptionUtils;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RoutingAuditPublisher<T extends AuditEvent> {

    private static CustomLogger log = LoggerUtils.getLogger(RoutingAuditPublisher.class.getName());

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

    private synchronized void publish(Supplier<CommonClientInterface> supplier, List<T> events, String error)  {
        try {
            supplier.get().audit(events.stream().map(converter::extend).collect(Collectors.toList()));
        } catch (Exception e) {
            events.forEach(event -> callback.onFailure(event, FailReport.CLIENT_INITIALIZATION_ERROR, String.format("%s Exception: %s", error, ExceptionUtils.toString(e))));
        }
    }

    public void shutdown() {
        tenantCacheProxy.shutDown();
    }

    public void gracefulShutdown() throws Exception {
        tenantCacheProxy.gracefulShutdown();
    }

}
