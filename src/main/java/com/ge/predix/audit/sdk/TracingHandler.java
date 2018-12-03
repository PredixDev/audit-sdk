package com.ge.predix.audit.sdk;

import java.util.Optional;

import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditTracingEvent;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.eventhub.stub.Ack;

/**
 * Created by 212582776 on 2/21/2018.
 */
public interface TracingHandler {

    /**
     * @return AuditTracingEvent optional.
     * it will return the sent message or null if no message was sent
     */
    Optional<AuditTracingEvent> sendInitialCheckpoint();

    void sendCheckpoint(Ack ack);

    void sendCheckpoint(AuditEvent event, LifeCycleEnum lifeCycleStatus, String message);

    boolean isTracingAck(Ack ack);

    boolean isTracingEvent(AuditEvent event);
}
