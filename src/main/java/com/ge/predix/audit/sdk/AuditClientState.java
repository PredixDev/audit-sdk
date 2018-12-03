package com.ge.predix.audit.sdk;

/**
 * Created by 212582776 on 2/26/2018.
 */
public enum AuditClientState {

    /**
     *  The audit client is in the middle of creation process.
     */
    CONNECTING,
    /**
     * The audit client is connected and can be used.
     */
    CONNECTED,
    /**
     * The audit client is disconnected.
     */
    DISCONNECTED,
    /**
     * The audit client was shutdown. It cannot be used any longer.
     */
    SHUTDOWN

}
