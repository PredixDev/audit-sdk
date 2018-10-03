package com.ge.predix.audit.sdk.routing.tms;

import com.ge.predix.audit.sdk.config.vcap.AuditServiceCredentials;
import com.ge.predix.audit.sdk.exception.TmsClientException;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class AuditTmsClient {

    private static CustomLogger log = LoggerUtils.getLogger(AuditTmsClient.class.getName());

    private final TmsClient client;
    private final String canonicalServiceName;

    public Optional<TmsServiceInstance<AuditServiceCredentials>> fetchServiceInstance(String tenantUuid) throws TmsClientException {
        log.info("Trying to fetch audit service instance from TMS, tenantUuid %s", tenantUuid);
       return client.fetchServiceInstance(tenantUuid, AuditServiceCredentials.class, canonicalServiceName);
    }

}
