package com.ge.predix.audit.sdk;

/**
 * Created by Igor on 11/01/2017.
 */
public enum FailReport {
    BAD_ACK,
    NO_ACK,
    NO_MORE_RETRY,
    JSON_ERROR,
    VERSION_NOT_SUPPORTED,
    ADD_MESSAGE_ERROR,
    STREAM_IS_CLOSE,
    RECONNECT_FAILURE,
    VALIDATION_ERROR,
    AUTHENTICATION_FAILURE,
    CACHE_IS_FULL,
    CLIENT_INITIALIZATION_ERROR
}