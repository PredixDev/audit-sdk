package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.message.AuditEvent;

import java.util.List;

/**
 * Created by Igor on 11/01/2017.
 */

/**
 * Callback of Audit Events
 */
public interface AuditCallback<T extends AuditEvent> {
	
	/**
	 * A callback to notify the failures of the auditing action
	 * @param result The failures report that contains the list of the failed events and the failure reason 
	 */
    void onFailure(AuditAsyncResult<T> result);
    
    /**
     * A callback to notify a general client error (e.g. authentication error)
     * @param report The client error reason
     * @param description The exception details of the error
     */
    void onClientError(ClientErrorCode report, String description);
    
    /**
     * A callback to notify the success of the auditing action
     * @param events The list of the successfully audited events
     */
    void onSuccess(List<T> events);
}