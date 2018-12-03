package com.ge.predix.audit.sdk;

/**
 * Created by 212582776 on 2/12/2018.
 */
public enum AuditCommonClientState {
	NOT_INIT,       // initial state
	CONNECTING,     // while reconnecting/ building client
	CONNECTED,      // we finished building the client successfully
	ACKED,          // we got an ack confirming the connection
	DISCONNECTED,   // after we got a "stream closed" onClientError call back
	SHUTDOWN        // client was shut-down. a non-recoverable action.
}
