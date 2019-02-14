package com.ge.predix.audit.sdk.routing;



import com.ge.predix.audit.sdk.ClientErrorCode;
import com.ge.predix.audit.sdk.AuditAsyncResult;
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
    
	/**
	 * A callback to notify the failures of the auditing action
	 * @param result The failures report that contains the list of the failed events and the failure reason 
	 */
	void onFailure(AuditAsyncResult<T> result);

    /**
     * For general error such as authentication or connection problems of specific tenant/shared audit client connection
		 * when using RoutingAuditClient, there is no need to do anything, this client will try to handle any error
		 * When using AuditClient you need to check the error and handle according to the configuration.
     * In any case, it is recommended to log the error.
     * @param report indicates what happened with the connection
     * @param description the description of the error. can be stacktrace or internal exception.
		 * @param auditServiceId the audit zone id connection that has this error
		 * @param tenantUuid the tenant id in TMS. for shared tenant, the tenantUuid will be null
     */
    void onClientError(ClientErrorCode report, String description, String auditServiceId, @Nullable String tenantUuid);
    
    /**
     * A callback to notify the success of the auditing action
     * @param events The list of the successfully audited events
     */
    void onSuccess(List<T> events);
}