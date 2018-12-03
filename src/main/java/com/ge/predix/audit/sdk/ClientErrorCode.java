package com.ge.predix.audit.sdk;

/**
 * Created by 212554562 on 9/4/2018.
 */
public enum ClientErrorCode {
	/**
	 * The stream is closed, and the state of the client is disconnected.
	 */
    STREAM_IS_CLOSE,
    
    /**
     * There is an authentication error in Eventhub due to a problem retrieving an OAuth token.
     */
    AUTHENTICATION_FAILURE,
    
    /**
     * Could not reconnect the Eventhub client
     */
    RECONNECT_FAILURE
}
