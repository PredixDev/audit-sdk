package com.ge.predix.audit.sdk;


import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 212584872 on 1/18/2017.
 */
@ToString
public class TestHelper implements AuditCallback{

    Log log = LogFactory.getLog(TestHelper.class);

    private AtomicInteger validateCount;
    private AtomicInteger failureCommonCount;
    private AtomicInteger failureCount;
    private AtomicInteger successCount;
    public AuditEvent lastFailureEvent, lastSuccessEvent;
    String lastFailureDescription;
    FailReport lastFailureCode;

    public TestHelper(){
        validateCount = new AtomicInteger(0);
        failureCommonCount = new AtomicInteger(0);
        failureCount = new AtomicInteger(0);
        successCount = new AtomicInteger(0);

    }

    @Override
    public void onValidate(AuditEvent event, List<ValidatorReport> reports) {
        log.info("onValidate: "+event);
        validateCount.incrementAndGet();
    }

    @Override
    public void onFailure(AuditEvent event, FailReport report, String description) {
        log.info("Failreport: "+report+" desc: "+description +" "+event);
        failureCount.incrementAndGet();
        lastFailureEvent = event;
        lastFailureDescription = description;
        lastFailureCode = report;
    }

    @Override
    public void onFailure(FailReport report, String description) {
        log.info("Failreport: "+report+" desc: "+description);
        failureCommonCount.incrementAndGet();

    }

    @Override
    public void onSuccees(AuditEvent event) {
        log.info("onSuccees: "+event);
        successCount.incrementAndGet();
        lastSuccessEvent = event;
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
