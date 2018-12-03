package com.ge.predix.audit.sdk;


import com.ge.predix.audit.sdk.message.AuditEvent;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 212584872 on 1/18/2017.
 */
@ToString
public class TestHelper<T extends AuditEvent> implements AuditCallback<T> {

    Log log = LogFactory.getLog(TestHelper.class);

    private AtomicInteger validateCount;
    private AtomicInteger failureCommonCount;
    private AtomicInteger failureCount;
    private AtomicInteger successCount;
    public AuditEvent lastFailureEvent;
    public List<AuditEvent> lastSuccessEventsBatch;
    String lastFailureDescription;
    Throwable lastFailureExcepion;
    FailCode lastFailureCode;
    ClientErrorCode lastClientErrorCode;
    List<AuditEventFailReport<T>> failReports;

    public TestHelper(){
        validateCount = new AtomicInteger(0);
        failureCommonCount = new AtomicInteger(0);
        failureCount = new AtomicInteger(0);
        successCount = new AtomicInteger(0);

    }

    @Override
    public void onFailure(AuditAsyncResult<T> result) {
        this.failReports = result.getFailReports();
        AuditEventFailReport<T> report = failReports.iterator().next();
        lastFailureCode = report.getFailureReason();
        lastFailureDescription = report.getDescription();
        lastFailureExcepion = report.getThrowable();
        lastFailureEvent = report.getAuditEvent();
        log.info("Failreport: "+ lastFailureCode +" desc: "+ lastFailureDescription +" "+ lastFailureEvent);
        failureCount.addAndGet(failReports.size());
    }

    @Override
    public void onClientError(ClientErrorCode clientErrorCode, String description) {
        log.info("ClientErrorCode: "+clientErrorCode+" desc: "+description);
        failureCommonCount.incrementAndGet();
        lastClientErrorCode = clientErrorCode;
    }

    @Override
    public void onSuccess(List events) {
        log.info("onSuccess: "+ events);
        successCount.addAndGet(events.size());
        lastSuccessEventsBatch = events;
    }

    public int getFailureCount(){
        return failureCount.get();
    }

    public int getFailures(){
        return failureCommonCount.get() + failureCount.get();
    }

    public int getSum(){
        return validateCount.get() +
                failureCommonCount.get() +
                failureCount.get() +
                successCount.get();
    }

    public int getValidateCount() {
        return validateCount.get();
    }

    public int getSuccessCount() {
        return successCount.get();
    }
}
