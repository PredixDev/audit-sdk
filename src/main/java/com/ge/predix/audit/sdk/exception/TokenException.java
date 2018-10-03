package com.ge.predix.audit.sdk.exception;

public class TokenException extends RuntimeException {

    public TokenException(Exception cause, String message) {
        super(message, cause);
    }

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Object... args) {
        super(String.format(message, args));
    }

    public TokenException(Exception e) {
        super(e);
    }




}
