package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.exception.UnmodifiableFieldException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV1;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Martin Saad on 1/30/2017.
 */
@Data
@Builder(builderClassName = "EventContainerBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class EventContainer {
    private AuditEvent auditEvent;
    private AtomicInteger numberOfRetriesMade = new AtomicInteger(0);
    private AuditEventFailReport auditEventFailReport;

     EventContainer(AuditEvent auditEvent){
        this.auditEvent = auditEvent;
    }

     int incrementAndGet(){
        return numberOfRetriesMade.incrementAndGet();
    }

     void setFailReport(FailReport failReport, String description){
        this.auditEventFailReport = AuditEventFailReport.builder()
                .auditEvent(this.auditEvent)
                .failureReason(failReport)
                .description(description)
                .build();
    }

     static class EventContainerBuilder {
        private AtomicInteger numberOfRetriesMade = new AtomicInteger(0);
    }
}
