package com.travelmanagementsystem.shared.exception;

public class ConflictException extends DomainException {

	protected ConflictException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static ConflictException of(String errorCode, String message) {
		return new ConflictException(errorCode, message);
	}

	public static ConflictException of(String message) {
		return new ConflictException("CONFLICT", message);
	}
}
