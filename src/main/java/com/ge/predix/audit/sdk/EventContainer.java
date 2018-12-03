package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.message.AuditEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Martin Saad on 1/30/2017.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventContainer<T extends AuditEvent> {
    private T auditEvent;
    private AtomicInteger numberOfRetriesMade = new AtomicInteger(0);
    private AuditEventFailReport<T> auditEventFailReport;

     EventContainer(T auditEvent){
        this.auditEvent = auditEvent;
    }

     int incrementAndGet(){
        return numberOfRetriesMade.incrementAndGet();
    }

     void setFailReport(FailCode failCode, String description, Throwable e) {
        this.auditEventFailReport = AuditEventFailReport.<T>builder()
                .auditEvent(this.auditEvent)
                .failureReason(failCode)
                .description(description)
                .throwable(e)
                .build();
    }
}
