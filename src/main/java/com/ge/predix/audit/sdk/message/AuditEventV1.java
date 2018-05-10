package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.exception.UnmodifiableFieldException;
import com.ge.predix.audit.sdk.message.validator.Uuid;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.UUID;

@Builder(builderClassName = "AuditEventV1Builder")
@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class AuditEventV1 implements AuditEvent {
    public static final int VERSION_1 = 1;

    @NotNull
    @Uuid
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String messageId;

    private int version = VERSION_1;

    private long timestamp;

    @Length(max = 36, message = "The field must be maximum 36 characters")
    private String tenantUuid;
    @Uuid
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String actorUuid;
    @Uuid
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String resourceUuid;
    @Uuid
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String batchUuid;

    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String originator;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String actionType;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String description;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String actor;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String actorDisplayName;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String resource;

    private long eventAt;
    private long eventsAt;

    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param1;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param2;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param3;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param4;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param5;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param6;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param7;
    @Length(max = 254, message = "The field must be maximum 254 characters")
    private String param8;

    public void setVersion(int version) {
        this.version = VERSION_1;
    }

    @ConstructorProperties({"messageId", "version", "timestamp", "tenantUuid", "actorUuid", "resourceUuid", "batchUuid", "originator", "actionType", "description", "actor", "actorDisplayName", "resource", "eventAt", "eventsAt", "param1", "param2", "param3", "param4", "param5", "param6", "param7", "param8"})
    public AuditEventV1(String messageId, int version, long timestamp, String tenantUuid, String actorUuid, String resourceUuid, String batchUuid, String originator, String actionType, String description, String actor, String actorDisplayName, String resource, long eventAt, long eventsAt, String param1, String param2, String param3, String param4, String param5, String param6, String param7, String param8) {
        if (messageId == null) {
            this.messageId = UUID.randomUUID().toString();
        } else {
            this.messageId = messageId;
        }
        if (timestamp > 0) {
            this.timestamp = timestamp;
        } else {
            this.timestamp = System.currentTimeMillis();
        }
        this.version = VERSION_1;
        this.tenantUuid = tenantUuid;
        this.actorUuid = actorUuid;
        this.resourceUuid = resourceUuid;
        this.batchUuid = batchUuid;
        this.originator = originator;
        this.actionType = actionType;
        this.description = description;
        this.actor = actor;
        this.actorDisplayName = actorDisplayName;
        this.resource = resource;
        this.eventAt = eventAt;
        this.eventsAt = eventsAt;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
        this.param5 = param5;
        this.param6 = param6;
        this.param7 = param7;
        this.param8 = param8;
    }

    @Override
    public AuditEvent clone() {
        return AuditEventV1.builder()
                .messageId(messageId)
                .timestamp(timestamp)
                .tenantUuid(tenantUuid)
                .actorUuid(actorUuid)
                .resourceUuid(resourceUuid)
                .batchUuid(batchUuid)
                .originator(originator)
                .actionType(actionType)
                .description(description)
                .actor(actor)
                .actorDisplayName(actorDisplayName)
                .resource(resource)
                .eventAt(eventAt)
                .eventsAt(eventsAt)
                .param1(param1)
                .param2(param2)
                .param3(param3)
                .param4(param4)
                .param5(param5)
                .param6(param6)
                .param7(param7)
                .param8(param8)
                .build();
    }

    public static class AuditEventV1Builder {
        private int version = VERSION_1;
        private String messageId = UUID.randomUUID().toString();
        private long timestamp = System.currentTimeMillis();

        public AuditEventV1Builder version(int version)
                throws UnmodifiableFieldException {
            throw new UnmodifiableFieldException("version");
        }
    }
}