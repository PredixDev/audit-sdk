package com.ge.predix.audit.sdk.message;


import com.ge.predix.audit.sdk.message.validator.Uuid;
import lombok.Data;
import lombok.Getter;

import java.util.UUID;

/**
 * @author Kobi {212584872} on 12/23/2016.
 * @author Igor {212579997}
 */

@Data
public class AuditTracingEvent implements AuditEvent {

    private static final int VERSION = -1;
    @Uuid
    private String messageId;

    @Uuid
    private String eventhubZoneId;

    private int version = VERSION;
    private long timestamp;
    @Getter
    private String tenantUuid = null;

    public AuditTracingEvent(String messageId, long timestamp, String eventhubZoneId) {
        this.messageId = (messageId == null)? UUID.randomUUID().toString() : messageId;
        this.timestamp = (timestamp > 0)? timestamp : System.currentTimeMillis();
        this.version = VERSION;
        this.eventhubZoneId = eventhubZoneId;
    }

    @Override
    public AuditEvent clone() {
        return new AuditTracingEvent(this.messageId,this.timestamp,this.eventhubZoneId);
    }

    public void setVersion(int version) {
        this.version = VERSION;
    }
}