package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.eventhub.EventHubClientException;

import java.util.List;

/**
 * Created by 212582776 on 2/20/2018.
 */
public interface CommonClientInterface {

	AuditingResult audit(AuditEvent event) throws AuditException;

	AuditingResult audit(List<AuditEvent> events) throws AuditException;

    void reconnect() throws EventHubClientException;

    void shutdown();

    void trace();

    AuditClientState getAuditClientState();

    void setAuthToken(String authToken) throws AuditException, EventHubClientException;

}
