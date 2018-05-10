package com.ge.predix.audit.sdk.exception;

public final class TracingException extends RuntimeException {
	public TracingException(final Throwable cause){
		super(cause);
	}
}