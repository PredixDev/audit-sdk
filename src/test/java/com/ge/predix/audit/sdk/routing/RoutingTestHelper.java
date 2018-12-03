package com.ge.predix.audit.sdk.routing;


import com.ge.predix.audit.sdk.*;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.routing.cache.impl.AuditAsyncClientHolder;
import com.ge.predix.audit.sdk.routing.tms.Token;
import com.google.common.collect.Maps;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by 212584872 on 1/18/2017.
 */
@ToString
@NoArgsConstructor
public class RoutingTestHelper<T extends AuditEvent> implements RoutingAuditCallback<T> {

    private final static Log log = LogFactory.getLog(RoutingTestHelper.class);
    Map<String, AuditRoutingCallbackKpis<T>> auditRoutingCallbackKpisMap = Maps.newHashMap();
    AuditRoutingCallbackKpis<T> sharedAuditKpis = new AuditRoutingCallbackKpis<>();

    public AuditRoutingCallbackKpis<T> getKpis(String tenantId) {
        return tenantId == null || tenantId.isEmpty()? sharedAuditKpis :
                auditRoutingCallbackKpisMap.computeIfAbsent(tenantId, (tenant)-> new AuditRoutingCallbackKpis<>());
    }

    @Override
    public void onFailure(AuditAsyncResult<T> result) {
        AuditEventFailReport<T> failReport = result.getFailReports().iterator().next();
        FailCode failCode = failReport.getFailureReason();
        String description = failReport.getDescription();
        T event =  failReport.getAuditEvent();
        log.info("Failreport: "+failCode+" desc: "+description +" AuditEvent: " + event);
        AuditRoutingCallbackKpis<T>  kpi = getKpis(event.getTenantUuid());
        kpi.getFailureCount().incrementAndGet();
        kpi.setLastFailureEvent(event);
        kpi.setLastFailureDescription(description);
        kpi.setLastFailureCode(failCode);
    }

    @Override
    public void onClientError(ClientErrorCode clientErrorCode, String description, String auditServiceId, @Nullable String tenantUuid) {
        log.info("Failreport: "+clientErrorCode+" desc: "+description + " tenantUuid: " + tenantUuid + " auditServiceId: "+auditServiceId);
        AuditRoutingCallbackKpis<T>  kpi = getKpis(tenantUuid);
        kpi.setAuditServiceId(auditServiceId);
        kpi.setLastFailureDescription(description);
        kpi.setLastClientErrorCode(clientErrorCode);
        kpi.getFailureCommonCount().incrementAndGet();
    }

    @Override
    public void onSuccess(List<T> events) {
        AuditRoutingCallbackKpis<T>  kpi = getKpis(events.iterator().next().getTenantUuid());
        kpi.getSuccessCount().incrementAndGet();
    }

    //Method that generates spy of AsyncClientHolders. the integers will be the expiration time in seconds
    public static List<AuditAsyncClientHolder> generateHolders(List<Integer> secondsToExpire) {
        String TENANT = "TENANT";
        String AUDIT_ZONE_ID = "AUDIT_ZONE_ID";
        ArrayList<AuditAsyncClientHolder> result = new ArrayList<>();
        for (int i = 0; i < secondsToExpire.size(); i++) { //for each index
            result.add(spy(new AuditAsyncClientHolder(  //insert new holder with the index name and set the expiration time to be the list element
                    mock(AuditClientAsyncImpl.class),
                    TENANT+i,
                    AUDIT_ZONE_ID+i,
                    generateToken(i, secondsToExpire.get(i)))
            ));
        }
        return result;
    }

    //Method that generates token
    public static Token generateToken(int position, int secondsToExpire) {
        return  new Token("access"+position, "bearer"+position, secondsToExpire, "audit"+position, "jti"+position);
    }
}
