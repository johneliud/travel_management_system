package com.travelmanagementsystem.shared.api;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
	String code,
	String message,
	Instant timestamp,
	String path,
	String correlationId,
	List<FieldError> errors) {

	public record FieldError(String field, String message) {}

	public static ErrorResponse of(String code, String message, String path) {
		return new ErrorResponse(code, message, Instant.now(), path, null, null);
	}

	public static ErrorResponse of(String code, String message, String path, List<FieldError> errors) {
		return new ErrorResponse(code, message, Instant.now(), path, null, errors);
	}

	public static ErrorResponse of(String code, String message, String path, String correlationId) {
		return new ErrorResponse(code, message, Instant.now(), path, correlationId, null);
	}
}
