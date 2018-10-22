package com.ge.predix.audit.sdk;

import java.util.*;

import com.ge.predix.audit.sdk.message.AuditEvent;

 class AuditingResultContainer<T extends AuditEvent> {
	
	private Map<String, AuditEventFailReport<T>> failedEvents = new HashMap<>();
	private Set<T> sentEvents = new HashSet<>();
	
	 AuditingResult<T> getResult() {
		return new AuditingResult<>(new ArrayList<>(failedEvents.values()), new ArrayList<>(sentEvents));
	}

	 void onFailure(T event, FailCode failReason, String description) {
		 onFailure(event, failReason, description, null);
	 }

	 void onFailure(T event, FailCode failReason, String description, Throwable throwable) {
		AuditEventFailReport<T> failedEvent = failedEvents.get(event.getMessageId());
		if (failedEvent == null) {
			failedEvents.put(event.getMessageId(), AuditEventFailReport.<T>builder()
					.auditEvent(event)
					.failureReason(failReason)
					.description(description)
					.throwable(throwable)
					.build());
		} else {
			failedEvent.setFailureReason(failReason);
			failedEvent.setDescription(description);
			failedEvent.setThrowable(throwable);
		}
	}
	
	 void onSuccess(T event) {
		failedEvents.remove(event.getMessageId());
		sentEvents.add(event);
	}
}
