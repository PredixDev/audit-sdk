package com.ge.predix.audit.sdk.routing.cache.impl;

import com.ge.predix.audit.sdk.AuditClientAsyncImpl;
import com.ge.predix.audit.sdk.exception.TokenException;
import com.ge.predix.audit.sdk.routing.tms.AuditTokenServiceClient;
import com.ge.predix.audit.sdk.routing.tms.Token;
import com.ge.predix.audit.sdk.util.CustomLogger;
import com.ge.predix.audit.sdk.util.LoggerUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@EqualsAndHashCode
public class AuditAsyncClientHolder {

    private static CustomLogger log = LoggerUtils.getLogger(AuditAsyncClientHolder.class.getName());

    @Getter private final AuditClientAsyncImpl client;
    @Getter private final String tenantUuid;
    @Getter private final String auditZoneId;
    private Token token;

    public synchronized AuditAsyncClientHolder refreshToken(AuditTokenServiceClient  tokenClient, long threshold) throws TokenException {
        if (this.token == null || this.token.shouldExpire(threshold)){
            setNewToken(tokenClient.getToken(this.tenantUuid));
        }
        return this;
    }

    public synchronized AuditAsyncClientHolder refreshNewToken(AuditTokenServiceClient  tokenClient) throws TokenException {
        setNewToken(tokenClient.getToken(this.tenantUuid));
        return this;
    }

    private void setNewToken(Token token) throws TokenException {
        try {
            log.info("Refreshing audit token for tenantUuid %s since existing token is null or going to be expired", tenantUuid);
            this.token = token;
            client.setAuthToken(token.getAccessToken());
        } catch (Exception e ) {
            throw new TokenException(e, String.format("Failed to refresh token for tenantUuid %s and audit-zone-id %s", tenantUuid, auditZoneId));
        }
    }
}
