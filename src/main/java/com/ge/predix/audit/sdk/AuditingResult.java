package com.ge.predix.audit.sdk;

import java.util.Collections;
import java.util.List;

import com.ge.predix.audit.sdk.message.AuditEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
public class AuditingResult {

	private static final AuditingResult empty = new AuditingResult(Collections.emptyList(), Collections.emptyList());

	private List<AuditEventFailReport> failedEvents;
	private List<AuditEvent> sentEvents;

	public static AuditingResult emptyResults(){
		return empty;
	}
}
