package com.ge.predix.audit.sdk.exception;

/**
 * Created by Igor on 11/01/2017.
 */
public class AuditException extends Exception {
    public AuditException(String message) {
        super(message);
    }

    public AuditException(String message, Exception e) {
        super(message, e);
    }
}
