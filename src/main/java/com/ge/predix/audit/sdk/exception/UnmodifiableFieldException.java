package com.ge.predix.audit.sdk.exception;

public class UnmodifiableFieldException extends Exception {
	public UnmodifiableFieldException(String message) {
		super(String.format("The field{%s} is protected", message));
	}
}
