package com.ge.predix.audit.sdk.message;


import lombok.*;
import org.hibernate.validator.constraints.Length;



@ToString(callSuper = true)
@Getter
public class AuditEventV2Extended extends AuditEventV2 implements AuditEventExtended {

    @Length(max = 100, message = "The field must be maximum 100 characters")
    private final String appName;

     AuditEventV2Extended(AuditEventV2 eventV2, String appName) {
        super(  eventV2.getMessageId(),
                eventV2.getVersion(),
                eventV2.getTimestamp(),
                eventV2.getClassifier(),
                eventV2.getPublisherType(),
                eventV2.getCategoryType(),
                eventV2.getEventType(),
                eventV2.getPayload(),
                eventV2.getCorrelationId(),
                eventV2.getTenantUuid());

        this.appName = appName;
    }

    @Override
    public AuditEventV2Extended clone() {
        return new AuditEventV2Extended(super.clone(), appName);
    }



}
