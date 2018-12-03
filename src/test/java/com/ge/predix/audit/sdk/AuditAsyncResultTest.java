package com.ge.predix.audit.sdk;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEventV2;

/**
 * Created by 212554562 on 10/2/2018.
 */
public class AuditAsyncResultTest {

	private int NUM_OF_EVENTS = 2;
	
    @Test
    public void getSummary() {
    	String badAckDesc = "bad ack";
    	String validDesc = "validation: bad id";
    	String noAckDesc = "no ack error";
    	Exception clientInitException = new Exception("client init error throwable message");
    	String errorPartialString = "message: client init error throwable message Cause: null Stacktrace:";
    	
        List<AuditEventFailReport<AuditEvent>> failReports = new ArrayList<>();
        
		failReports.addAll(geteventFailReport(FailCode.BAD_ACK, badAckDesc, null));
		failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, validDesc, null));
        failReports.addAll(geteventFailReport(FailCode.VALIDATION_ERROR, null, null));
		failReports.addAll(geteventFailReport(FailCode.NO_ACK, noAckDesc, null));
		failReports.addAll(geteventFailReport(FailCode.CLIENT_INITIALIZATION_ERROR, "init_error", clientInitException));

        AuditAsyncResult<AuditEvent> result = AuditAsyncResult.builder().failReports(failReports).build();

        String summary = result.getSummary();
        
        assertEquals(NUM_OF_EVENTS, countOccurrences(summary, badAckDesc));
        assertEquals(NUM_OF_EVENTS, countOccurrences(summary, validDesc));
        assertEquals(NUM_OF_EVENTS, countOccurrences(summary, "Validation report: null"));
        assertEquals(1, countOccurrences(summary, errorPartialString));
        assertEquals(0, countOccurrences(summary, noAckDesc));
        assertEquals(1, countOccurrences(summary, "did not receive ACKs"));
        
        System.out.println(result);
    }


	private int countOccurrences(String fullString, String findStr) {
		return (fullString.length() - fullString.replaceAll(findStr, "").length()) / findStr.length();
	}

    private List<AuditEventFailReport<AuditEvent>> geteventFailReport(FailCode code, String description, Throwable t) {
        List<AuditEventFailReport<AuditEvent>> list = new ArrayList<>();

        for(int i = 0 ; i < NUM_OF_EVENTS ; i++) {
            list.add(AuditEventFailReport.builder()
                    .auditEvent(AuditEventV2.builder()
                            .categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
                            .eventType(AuditEnums.EventType.STARTUP_EVENT)
                            .classifier(AuditEnums.Classifier.FAILURE)
                            .publisherType(AuditEnums.PublisherType.APP_SERVICE)
                            .build())
                    .failureReason(code)
                    .description(description)
                    .throwable(t)
                    .build());
        }

        return list;
    }
}
