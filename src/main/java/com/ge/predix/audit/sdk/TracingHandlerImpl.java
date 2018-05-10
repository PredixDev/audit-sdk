package com.ge.predix.audit.sdk;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.TracingException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditTracingEvent;
import com.ge.predix.audit.sdk.message.tracing.Checkpoint;
import com.ge.predix.audit.sdk.message.tracing.LifeCycleEnum;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSender;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSenderImpl;
import com.ge.predix.audit.sdk.message.tracing.TracingMetaData;
import com.ge.predix.eventhub.Ack;
import com.ge.predix.eventhub.AckStatus;

import lombok.Getter;

import static com.ge.predix.audit.sdk.AbstractAuditClientImpl.printAck;

/**
 * Created by 212582776 on 2/11/2018.
 */
public class TracingHandlerImpl implements TracingHandler {

    @Getter
    private static Logger log = Logger.getLogger(TracingHandlerImpl.class.getName());
    public final AuditConfiguration configuration;
    private TracingMessageSender tracingMessageSender;
    protected AtomicReference<AuditTracingEvent> auditTracingEvent = new AtomicReference<>();
    private ObjectMapper om;

    public TracingHandlerImpl(AuditConfiguration configuration) throws AuditException {
        this.configuration = configuration;
        this.tracingMessageSender  = buildTracingMessageSender();
        setAuditTracingEvent();
        om = new ObjectMapper();

    }

    @Override
    public void sendCheckpoint(Ack ack) {
        LifeCycleEnum state = (ack.getStatusCode() == AckStatus.ACCEPTED)?
                LifeCycleEnum.CHECK: LifeCycleEnum.FAIL;
        String customData = String.format("EventHub ack: %s", printAck(ack));
        Checkpoint messageToSend = buildTracingMessage(this.auditTracingEvent.get(),
                state, customData);
        tracingMessageSender.sendTracingMessage(messageToSend);
    }

    @Override
    public boolean isTracingEvent(AuditEvent event) {
        return this.auditTracingEvent.get().getMessageId().equals(event.getMessageId());
    }

    @Override
    public void sendCheckpoint(AuditEvent event, LifeCycleEnum lifeCycleStatus, String message) {
        log.info("found tracing event. notifying LifeCycle FAIL");
        Checkpoint messageToSend = buildTracingMessage(this.auditTracingEvent.get(), lifeCycleStatus, message);
        tracingMessageSender.sendTracingMessage(messageToSend);
    }

    @Override
    public boolean isTracingAck(Ack  ack) {
        if(ack == null){
            return false;
        }
        return this.auditTracingEvent.get().getMessageId().equals(ack.getId());
    }

    @Override
    public Optional<AuditTracingEvent> sendInitialCheckpoint() {
        try {
            getAuditTracingEvent().setMessageId(UUID.randomUUID().toString());
            log.info("Starting tracing flow");
            Checkpoint checkpoint = buildTracingMessage(this.auditTracingEvent.get(), LifeCycleEnum.START,"onSend");
            this.tracingMessageSender.sendTracingMessage(checkpoint);
            log.info("calling Audit from tracing interval with event: " + auditTracingEvent.get());
        } catch (Exception e) {
            log.info(new TracingException(e).toString());
        }
        return Optional.of(getAuditTracingEvent());

    }

    protected  AuditTracingEvent getAuditTracingEvent() {
        return auditTracingEvent.get();
    }

    private void setAuditTracingEvent() {
        auditTracingEvent.set(new AuditTracingEvent(UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                this.configuration.getEhubZoneId()));
    }

    private TracingMessageSender buildTracingMessageSender(){
        TracingMessageSender tracingMessageSender;
        try {
            tracingMessageSender = new TracingMessageSenderImpl(
                    configuration.getTracingUrl(),
                    configuration.getTracingToken());
        } catch (URISyntaxException e) {
            log.warning("tracing URL is invalid: "+configuration.getTracingUrl());
            return null;
        }
        return tracingMessageSender;
    }

    protected Checkpoint buildTracingMessage(AuditTracingEvent auditTracingEvent, LifeCycleEnum lifeCycle, String customData) {
        String appName = (configuration.getAppName() != null)? configuration.getAppName() : "";
        String space = (configuration.getSpaceName() != null)? configuration.getSpaceName() : "";
        String auditName = (configuration.getAuditServiceName() != null)? configuration.getAuditServiceName() : "";

        String value;
        TracingMetaData tracingMetaData = TracingMetaData.builder()
                .auditClientType(this.configuration.getClientType())
                .bulkMode(this.configuration.getBulkMode())
                .eventhubHost(this.configuration.getEhubHost())
                .uaaUrl(this.configuration.getUaaUrl())
                .customData(customData)
                .appName(appName)
                .auditServiceName(auditName)
                .spaceName(space)
                .reconnectMode(this.configuration.getReconnectMode())
                .retryCount(this.configuration.getMaxRetryCount())
                .retryInterval(this.configuration.getRetryIntervalMillis())
                .cacheSize(this.configuration.getMaxNumberOfEventsInCache())
                .authenticationMethod(this.configuration.getAuthenticationMethod())
                .build();
        try {
            value = om.writeValueAsString(tracingMetaData);
        } catch (JsonProcessingException e) {
           value = tracingMetaData.toString();
        }
        return   Checkpoint.builder()
                .tenantId(this.configuration.getEhubZoneId())
                .flowId(auditTracingEvent.getMessageId())
                .state(lifeCycle)
                .payload(value)
                .build();
    }
}
