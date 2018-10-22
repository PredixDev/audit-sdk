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
public class AuditingResult<T extends AuditEvent> {

	private List<AuditEventFailReport<T>> failedEvents;
	private List<T> sentEvents;

	public static <T extends AuditEvent> AuditingResult<T> emptyResults(){
		 return new AuditingResult<T>(Collections.emptyList(), Collections.emptyList());
	}
}
