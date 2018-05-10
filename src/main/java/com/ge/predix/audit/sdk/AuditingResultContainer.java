package com.ge.predix.audit.sdk;

import java.util.*;

import com.ge.predix.audit.sdk.message.AuditEvent;

 class AuditingResultContainer {
	
	private Map<String, AuditEventFailReport> failedEvents = new HashMap<>();
	private Set<AuditEvent> sentEvents = new HashSet<>();
	
	 AuditingResult getResult() {
		return new AuditingResult(new ArrayList<AuditEventFailReport>(failedEvents.values()), new ArrayList<>(sentEvents));
	}
	
	 void onFailure(AuditEvent event, FailReport failReason, String description) {
		AuditEventFailReport failedEvent = failedEvents.get(event.getMessageId());
		if (failedEvent == null) {
			failedEvents.put(event.getMessageId(), AuditEventFailReport.builder()
					.auditEvent(event)
					.failureReason(failReason)
					.description(description)
					.build());
		} else {
			failedEvent.setFailureReason(failReason);
			failedEvent.setDescription(description);
		}
	}
	
	 void onSuccess(AuditEvent event) {
		failedEvents.remove(event.getMessageId());
		sentEvents.add(event);
	}
}
