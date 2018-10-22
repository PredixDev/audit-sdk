package com.ge.predix.audit.sdk;



import com.ge.predix.audit.sdk.message.AuditEvent;

import java.util.List;

/**
 * Created by Igor on 11/01/2017.
 */
public interface AuditCallback<T extends AuditEvent> {
    void onFailure(Result<T> result);
    void onClientError(ClientErrorCode report, String description);
    void onSuccess(List<T> events);
}