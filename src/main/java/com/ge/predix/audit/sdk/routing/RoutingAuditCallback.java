package com.ge.predix.audit.sdk.routing;



import com.ge.predix.audit.sdk.ClientErrorCode;
import com.ge.predix.audit.sdk.Result;
import com.ge.predix.audit.sdk.message.AuditEvent;


import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by Igor on 11/01/2017.
 */

/**
 *Callback of Audit Events
 */
public interface RoutingAuditCallback<T extends AuditEvent> {
    void onFailure(Result<T> result);

    /**
     * For general error such as authorization or connection problems of specific tenant/shared audit client connection
     * For errors of the shared audit instance the tenantUuid will be null.
     */
    void onClientError(ClientErrorCode report, String description, String auditServiceId, @Nullable String tenantUuid);
    void onSuccess(List<T> events);
}