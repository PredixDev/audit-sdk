package com.ge.predix.audit.sdk.exception;

/**
 * Created by Igor on 11/01/2017.
 */
public class VcapLoadException extends Exception {
    public VcapLoadException(String message) {
        super(message);
    }

    public VcapLoadException(Exception e) {
        super(e);
    }
}
