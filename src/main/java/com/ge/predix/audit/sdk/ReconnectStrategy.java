package com.ge.predix.audit.sdk;

/**
 * Created by 212582776 on 2/12/2018.
 */
@FunctionalInterface
public interface ReconnectStrategy {

    /**Notifies the Strategy of a change in the client's state.
     *
     * @param auditCommonClientState - the new state of the audit client
     */
    void notifyStateChanged(AuditCommonClientState auditCommonClientState);
}
