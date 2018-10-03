package com.ge.predix.audit.sdk.exception;

public class RoutingAuditException extends RuntimeException {

    public RoutingAuditException(String message, Exception cause) {
        super(message, cause);
    }

    public RoutingAuditException(String message) {
        super(message);
    }

    public RoutingAuditException(Exception e) {
        super(e);
    }




}
