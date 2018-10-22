package com.ge.predix.audit.sdk.message;

import com.ge.predix.audit.sdk.exception.AuditValidationException;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 * Created by 212554562 on 9/6/2018.
 */
public class AuditEventV2AppNameValidationTest {

    @Test
    public void auditNoPublisherTest(){

        String ilegalAppNam = "ilegalAppName                                                                         " +
                "                                                                                                   ";

        AuditEventV2 eventV2 = AuditEventV2.builder()
                .classifier(AuditEnums.Classifier.FAILURE)
                .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                .categoryType(AuditEnums.CategoryType.API_CALLS)
                .eventType(AuditEnums.EventType.FAILURE_API_REQUEST)
                .build();

        eventV2.setAppName(ilegalAppNam);

        List<ValidatorReport> report = ValidatorServiceImpl.instance.validate(eventV2);
        assertFalse(report.isEmpty());
    }
}
