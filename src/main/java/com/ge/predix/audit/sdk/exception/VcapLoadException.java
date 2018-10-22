package com.ge.predix.audit.sdk.exception;

/**
 * Created by Igor on 11/01/2017.
 */
public class VcapLoadException extends RuntimeException {
    public VcapLoadException(String message) {
        super(message);
    }

    public VcapLoadException(RuntimeException e) {
        super(e);
    }

}
