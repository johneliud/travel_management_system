package com.travelmanagementsystem.shared.exception;

public class NotFoundException extends DomainException {

	protected NotFoundException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static <T> NotFoundException of(Class<T> type, Object id) {
		return new NotFoundException(
			type.getSimpleName().toUpperCase() + "_NOT_FOUND",
			type.getSimpleName() + " not found with id: " + id);
	}
}
