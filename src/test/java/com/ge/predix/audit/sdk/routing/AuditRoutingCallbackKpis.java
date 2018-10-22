package com.ge.predix.audit.sdk.routing;

import com.ge.predix.audit.sdk.ClientErrorCode;
import com.ge.predix.audit.sdk.FailCode;
import com.ge.predix.audit.sdk.message.AuditEvent;
import lombok.*;

import java.util.concurrent.atomic.AtomicInteger;


@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class AuditRoutingCallbackKpis<T extends AuditEvent> {
    private String auditServiceId;
    private AtomicInteger failureCommonCount = new AtomicInteger();
    private AtomicInteger failureCount = new AtomicInteger();
    private AtomicInteger successCount = new AtomicInteger();
    private T lastFailureEvent;
    private T AuditEventlastSuccessEvent;
    private String lastFailureDescription;
    private FailCode lastFailureCode;
    private ClientErrorCode lastClientErrorCode;

    public int getFailures(){
        return failureCommonCount.get() + failureCount.get();
    }

    public int getSum(){
        return getFailures() + successCount.get();
    }

}
