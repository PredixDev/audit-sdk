package com.ge.predix.audit.sdk.routing.tms;

import com.ge.predix.audit.sdk.exception.TokenException;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuditTokenServiceClient {

    private static CustomLogger log = LoggerUtils.getLogger(AuditTokenServiceClient.class.getName());

    private final String canonicalServiceName;
    private final TokenServiceClient client;

    public Token getToken(String tenantUuid) throws TokenException {
        log.info("Trying to fetch new token from tokenService for tenantUuid %s", tenantUuid);
        return client.getToken(tenantUuid, canonicalServiceName);
    }

}
