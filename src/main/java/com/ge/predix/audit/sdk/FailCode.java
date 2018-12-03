package com.ge.predix.audit.sdk;

/**
 * Created by Igor on 11/01/2017.
 */
public enum FailCode {
	/**
	 * A bad ACK was received from Eventhub
	 */
    BAD_ACK,
    
    /**
     * No ACK has been received from Eventhub in a timely fashion
     */
    NO_ACK,
    
    /**
     * Failed to parse the audit event to JSON format
     */
    JSON_ERROR,
    
    /**
     * Failed to add audit event to Eventhub cache
     */
    ADD_MESSAGE_ERROR,
    
    /**
     * The audit event doesn't match some (or all) of the constraints (e.g. the payload is too long)
     */
	VALIDATION_ERROR,
    
    /**
     * The audit event was removed from the audit cache because it is full
     */
    CACHE_IS_FULL,
    
    /**
     * The client hasn't been initialized properly
     */
    CLIENT_INITIALIZATION_ERROR,
    
    /**
     * An audit event with the same message id has already been added to the audit cache
     */
    DUPLICATE_EVENT
}