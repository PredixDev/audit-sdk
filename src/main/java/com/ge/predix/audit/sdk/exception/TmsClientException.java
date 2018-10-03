package com.ge.predix.audit.sdk.exception;

public class TmsClientException extends RuntimeException {

    public TmsClientException(Exception cause, String message) {
        super(message, cause);
    }

    public TmsClientException(String message) {
        super(message);
    }

    public TmsClientException(Exception e) {
        super(e);
    }




}
