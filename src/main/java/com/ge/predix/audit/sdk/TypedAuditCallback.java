package com.ge.predix.audit.sdk;



import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.validator.ValidatorReport;

import java.util.List;

/**
 * Created by Igor on 11/01/2017.
 */
public interface TypedAuditCallback<T extends AuditEvent> {
    void onValidate(T event, List<ValidatorReport> reports);
    void onFailure(T event, FailReport report, String description);
    void onFailure(FailReport report, String description);
    void onSuccees(T event);
}