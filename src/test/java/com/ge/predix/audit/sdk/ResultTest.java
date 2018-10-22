package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.exception.AuditValidationException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Created by 212554562 on 10/2/2018.
 */
public class ResultTest {

    @Test
    public void getSummery(){

        List<AuditEventFailReport<AuditEvent>> failReports = new ArrayList<>();
        long badAckCount = 7;
        long validationBadIdcount = 3;
        long validationBadSomethingElseCount = 1;
        long validationBadSomethingElseNoDescriptionCount = 5;
        long validationBadSomethingElseNoExceptionCount = 2;
        int validationNoDescriptionNoThrowable = 6;
        String badAckDescription = "bad ack";
        String validationBadIdDescription = " validation : bad id";
        String validationBadSomethingElseDescription = " validation : bad something else";
        AuditException badAckThrowable = new AuditException("\"bad ack\"");
        AuditValidationException validationBadIdThrowable = new AuditValidationException(null);
        AuditValidationException validationBadSomethingElseThrowable = new AuditValidationException(null);
        failReports.addAll(geteventFailReport(FailCode.BAD_ACK, badAckDescription, badAckThrowable, badAckCount));
        failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, validationBadIdDescription, validationBadIdThrowable, validationBadIdcount));
        failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, validationBadSomethingElseDescription, validationBadSomethingElseThrowable, validationBadSomethingElseCount));
        failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, null, validationBadSomethingElseThrowable, validationBadSomethingElseNoDescriptionCount));
        failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, validationBadSomethingElseDescription, null, validationBadSomethingElseNoExceptionCount));
        failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, null, null, validationNoDescriptionNoThrowable));

        Result<AuditEvent> result = Result.builder().failReports(failReports).build();

        Map<FailCode, Set<Result.FailType>> summery = result.getSummery();

        assertTrue(badAckCount == get(summery.get(FailCode.BAD_ACK),new Result.FailType(badAckDescription, badAckThrowable)).getMessageIds().size());
        assertTrue(validationBadIdcount == get(summery.get(FailCode.VALIDATION_ERROR),new Result.FailType(validationBadIdDescription ,validationBadIdThrowable)).getMessageIds().size());
        assertTrue(validationBadSomethingElseCount ==
                get(summery.get(FailCode.VALIDATION_ERROR),new Result.FailType(validationBadSomethingElseDescription ,validationBadSomethingElseThrowable)).getMessageIds().size());
        assertTrue(validationBadSomethingElseNoDescriptionCount ==
                get(summery.get(FailCode.VALIDATION_ERROR),new Result.FailType(null ,validationBadSomethingElseThrowable)).getMessageIds().size());
        assertTrue(validationBadSomethingElseNoExceptionCount ==
                get(summery.get(FailCode.VALIDATION_ERROR),new Result.FailType(validationBadSomethingElseDescription ,null)).getMessageIds().size());
        assertTrue(validationNoDescriptionNoThrowable ==
                get(summery.get(FailCode.VALIDATION_ERROR),new Result.FailType(null ,null)).getMessageIds().size());

        System.out.println("summery = " + result);
    }


    private Result.FailType get(Set<Result.FailType> set , Result.FailType type){

        return set.stream()
                .filter(object -> object.equals(type))
                .findFirst().get();
    }

    private List<AuditEventFailReport<AuditEvent>> geteventFailReport(FailCode code, String description, Throwable throwable , long count) {
        List<AuditEventFailReport<AuditEvent>> list = new ArrayList<>();

        for(int i = 0 ; i< count ; i++) {
            list.add(AuditEventFailReport.builder()
                    .auditEvent(AuditEventV2.builder()
                            .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                            .eventType(AuditEnums.EventType.STARTUP_EVENT)
                            .classifier(AuditEnums.Classifier.FAILURE)
                            .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                            .build())
                    .failureReason(code)
                    .description(description)
                    .throwable(throwable)
                    .build());
        }

        return list;
    }
}
