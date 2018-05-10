package com.ge.predix.audit.sdk.message;

import java.io.Serializable;

/**
 * Created by Igor on 11/01/2017.
 */
public interface AuditEvent extends Serializable, Cloneable {
    int getVersion();
    void setMessageId(String messageId);
    String getMessageId();
    String getTenantUuid();
    AuditEvent clone();
}
