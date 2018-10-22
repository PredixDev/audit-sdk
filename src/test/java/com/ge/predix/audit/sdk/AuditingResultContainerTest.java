package com.ge.predix.audit.sdk;

import static org.junit.Assert.*;

import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import org.junit.Test;

import com.ge.predix.audit.sdk.message.AuditEvent;

public class AuditingResultContainerTest {
	
	AuditingResultContainer resultContainer = new AuditingResultContainer();

	AuditEventV2 auditEvent_1 = AuditEventV2.builder()
			.categoryType(AuditEnums.CategoryType.ADMINISTRATIONS)
			.eventType(AuditEnums.EventType.STARTUP_EVENT)
			.classifier(AuditEnums.Classifier.FAILURE)
			.publisherType(AuditEnums.PublisherType.APP_SERVICE)
			.build();

	@Test
	public void getResult_emptyResult() {
		AuditingResult result = resultContainer.getResult();
		
		assertTrue(result.getSentEvents().isEmpty());
		assertTrue(result.getFailedEvents().isEmpty());
	}
	
	@Test
	public void onFailure_newFailedEvent() {
		FailCode reason = FailCode.BAD_ACK;
		String description = "bad ack";
		
		resultContainer.onFailure(auditEvent_1, reason, description);
		
		AuditingResult<AuditEvent> result = resultContainer.getResult();
		assertTrue(result.getSentEvents().isEmpty());
		assertEquals(1, result.getFailedEvents().size());
		AuditEventFailReport report = result.getFailedEvents().get(0);
		assertEquals(auditEvent_1, report.getAuditEvent());
		assertEquals(reason, report.getFailureReason());
		assertEquals(description, report.getDescription());
	}
	
	@Test
	public void onFailure_failedEventAlreadyExists_reasonAndDescChange() {
		FailCode reason = FailCode.BAD_ACK;
		String description = "bad ack";
		FailCode reason2 = FailCode.NO_ACK;
		String description2 = "no ack";
		
		resultContainer.onFailure(auditEvent_1, reason, description);
		resultContainer.onFailure(auditEvent_1, reason2, description2);
		
		AuditingResult<AuditEvent> result = resultContainer.getResult();
		assertTrue(result.getSentEvents().isEmpty());
		assertEquals(1, result.getFailedEvents().size());
		AuditEventFailReport report = result.getFailedEvents().get(0);
		assertEquals(auditEvent_1, report.getAuditEvent());
		assertEquals(reason2, report.getFailureReason());
		assertEquals(description2, report.getDescription());
	}
	
	@Test
	public void onSuccess_newSuccessEvent() {
		
		resultContainer.onSuccess(auditEvent_1);
		
		AuditingResult<AuditEvent> result = resultContainer.getResult();
		assertTrue(result.getFailedEvents().isEmpty());
		assertEquals(1, result.getSentEvents().size());
		AuditEvent successEvent = result.getSentEvents().get(0);
		assertEquals(auditEvent_1, successEvent);
	}
	
	@Test
	public void onSuccess_successReplacingFailed() {
		FailCode reason = FailCode.BAD_ACK;
		String description = "bad ack";
		
		resultContainer.onFailure(auditEvent_1, reason, description);
		resultContainer.onSuccess(auditEvent_1);
		
		AuditingResult<AuditEvent> result = resultContainer.getResult();
		assertTrue(result.getFailedEvents().isEmpty());
		assertEquals(1, result.getSentEvents().size());
		AuditEvent successEvent = result.getSentEvents().get(0);
		assertEquals(auditEvent_1, successEvent);
	}
}
