package com.ge.predix.audit.sdk.exception;

public final class VersioningException extends RuntimeException {
	public VersioningException(final Throwable cause){
		super(cause);
	}
	public VersioningException(String string){
		super(string);
	}
}