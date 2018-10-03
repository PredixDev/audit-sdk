package com.ge.predix.audit.sdk;

import static org.junit.Assert.*;

import com.ge.predix.audit.sdk.message.AuditEventV2;
import org.junit.Test;

import com.ge.predix.audit.sdk.message.AuditEvent;

public class AuditingResultContainerTest {
	
	AuditingResultContainer resultContainer = new AuditingResultContainer();
	
	@Test
	public void getResult_emptyResult() {
		AuditingResult result = resultContainer.getResult();
		
		assertTrue(result.getSentEvents().isEmpty());
		assertTrue(result.getFailedEvents().isEmpty());
	}
	
	@Test
	public void onFailure_newFailedEvent() {
		AuditEvent event = new AuditEventV2();
		FailReport reason = FailReport.BAD_ACK;
		String description = "bad ack";
		
		resultContainer.onFailure(event, reason, description);
		
		AuditingResult result = resultContainer.getResult();
		assertTrue(result.getSentEvents().isEmpty());
		assertEquals(1, result.getFailedEvents().size());
		AuditEventFailReport report = result.getFailedEvents().get(0);
		assertEquals(event, report.getAuditEvent());
		assertEquals(reason, report.getFailureReason());
		assertEquals(description, report.getDescription());
	}
	
	@Test
	public void onFailure_failedEventAlreadyExists_reasonAndDescChange() {
		AuditEvent event = new AuditEventV2();
		FailReport reason = FailReport.BAD_ACK;
		String description = "bad ack";
		FailReport reason2 = FailReport.NO_ACK;
		String description2 = "no ack";
		
		resultContainer.onFailure(event, reason, description);
		resultContainer.onFailure(event, reason2, description2);
		
		AuditingResult result = resultContainer.getResult();
		assertTrue(result.getSentEvents().isEmpty());
		assertEquals(1, result.getFailedEvents().size());
		AuditEventFailReport report = result.getFailedEvents().get(0);
		assertEquals(event, report.getAuditEvent());
		assertEquals(reason2, report.getFailureReason());
		assertEquals(description2, report.getDescription());
	}
	
	@Test
	public void onSuccess_newSuccessEvent() {
		AuditEvent event = new AuditEventV2();
		
		resultContainer.onSuccess(event);
		
		AuditingResult result = resultContainer.getResult();
		assertTrue(result.getFailedEvents().isEmpty());
		assertEquals(1, result.getSentEvents().size());
		AuditEvent successEvent = result.getSentEvents().get(0);
		assertEquals(event, successEvent);
	}
	
	@Test
	public void onSuccess_successReplacingFailed() {
		AuditEvent event = new AuditEventV2();
		FailReport reason = FailReport.BAD_ACK;
		String description = "bad ack";
		
		resultContainer.onFailure(event, reason, description);
		resultContainer.onSuccess(event);
		
		AuditingResult result = resultContainer.getResult();
		assertTrue(result.getFailedEvents().isEmpty());
		assertEquals(1, result.getSentEvents().size());
		AuditEvent successEvent = result.getSentEvents().get(0);
		assertEquals(event, successEvent);
	}
}
