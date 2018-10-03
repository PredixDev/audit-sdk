package com.ge.predix.audit.sdk.routing;



import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.message.AuditEvent;


import javax.annotation.Nullable;

/**
 * Created by Igor on 11/01/2017.
 */

/**
 *Callback of Audit Events
 */
public interface RoutingAuditCallback<T extends AuditEvent> {
    void onFailure(T event, FailReport report, String description);

    /**
     * For general error such as authorization or connection problems.
     * For errors of the shared audit instance the tenantUuid will be null.
     */
    void onFailure(FailReport report, String description, String auditServiceId, @Nullable String tenantUuid);
    void onSuccess(T event);
}