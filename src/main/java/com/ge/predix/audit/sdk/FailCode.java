package com.ge.predix.audit.sdk;

/**
 * Created by Igor on 11/01/2017.
 */
public enum FailCode {
    BAD_ACK,
    NO_ACK,
    NO_MORE_RETRY,
    JSON_ERROR,
    ADD_MESSAGE_ERROR,
    VALIDATION_ERROR,
    CACHE_IS_FULL,
    CLIENT_INITIALIZATION_ERROR,
    DUPLICATE_EVENT
}