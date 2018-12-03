package com.ge.predix.audit.examples.service;

import com.ge.predix.audit.sdk.exception.AuditException;

/**
 * Created by Martin Saad on 2/9/2017.
 */
public interface PublisherService {
    String publishAsync() throws AuditException;
    String getLastResponse();
}
