package com.ge.predix.audit.sdk.message;

import io.netty.util.internal.StringUtil;

import java.io.Serializable;

/**
 * Created by Igor on 11/01/2017.
 */
public interface AuditEvent extends Serializable, Cloneable {
    int getVersion();
    String getMessageId();
    String getTenantUuid();

    /**
     * @return cloned Object
     */
    AuditEvent clone();
    default boolean hasTenantUuid() {
        return !StringUtil.isNullOrEmpty(this.getTenantUuid());
    }
}
