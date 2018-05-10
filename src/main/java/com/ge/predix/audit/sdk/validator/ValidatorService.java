package com.ge.predix.audit.sdk.validator;



import com.ge.predix.audit.sdk.exception.VersioningException;
import com.ge.predix.audit.sdk.message.AuditEvent;

import javax.xml.bind.ValidationException;
import java.util.List;


public interface ValidatorService {
    List<ValidatorReport> sanitize(AuditEvent audit) throws ValidationException;

    List<ValidatorReport> validate(AuditEvent event);

    boolean isValid(AuditEvent event) throws VersioningException;
}
