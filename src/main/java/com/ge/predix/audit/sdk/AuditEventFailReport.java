package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.message.AuditEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
@Builder
@AllArgsConstructor
public class AuditEventFailReport {
	private AuditEvent auditEvent;
	private FailReport failureReason;
	private String description;
}
