package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditTracingEvent;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.eventhub.Ack;


import java.util.Optional;

/**
 * Created by 212582776 on 3/6/2018.
 */
public class TracingHandleEmptyImpl implements TracingHandler {

        @Override
    public Optional<AuditTracingEvent> sendInitialCheckpoint() {
        return Optional.empty();
    }

    @Override
    public void sendCheckpoint(Ack ack) {
    }

    @Override
    public void sendCheckpoint(AuditEvent event, LifeCycleEnum lifeCycleStatus, String message) {
    }

    @Override
    public boolean isTracingAck(Ack ack) {
        return false;
    }

    @Override
    public boolean isTracingEvent(AuditEvent event) {
        return false;
    }
}
