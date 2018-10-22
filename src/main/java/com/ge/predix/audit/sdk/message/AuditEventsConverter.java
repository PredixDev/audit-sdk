package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.routing.tms.AppNameClient;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.function.Function;


public class AuditEventsConverter {

    private final Map<Class<? extends AuditEvent>, Function<AuditEvent, AuditEvent>> extendMap;

    public AuditEventsConverter(AppNameClient client) {
        extendMap = ImmutableMap.of(
                AuditEventV2.class, t -> {((AuditEventV2)t).setAppName(client.getAppName()); return t;},
                AuditTracingEvent.class, t -> t
        );
    }

    public <T extends AuditEvent>  AuditEvent extend(T t) {
        return extendMap.getOrDefault(t.getClass(), (event) -> {
            throw new IllegalArgumentException(String.format("Class %s not supported for event {%s}", event.getClass(), event));
        }).apply(t);
    }

}
