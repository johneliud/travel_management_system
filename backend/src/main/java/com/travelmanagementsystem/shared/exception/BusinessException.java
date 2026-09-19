package com.travelmanagementsystem.shared.exception;

public class BusinessException extends DomainException {

	protected BusinessException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static BusinessException of(String errorCode, String message) {
		return new BusinessException(errorCode, message);
	}
}
