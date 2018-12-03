package com.ge.predix.audit.sdk;

import javax.annotation.Nullable;

import com.ge.predix.audit.sdk.message.AuditEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
@Builder
@AllArgsConstructor
public class AuditEventFailReport<T extends AuditEvent> {
	private T auditEvent;
	private FailCode failureReason;
	private String description;
	@Nullable private Throwable throwable;
}
