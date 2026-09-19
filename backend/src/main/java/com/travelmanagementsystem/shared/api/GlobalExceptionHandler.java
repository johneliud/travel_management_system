package com.travelmanagementsystem.shared.api;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.travelmanagementsystem.shared.exception.BusinessException;
import com.travelmanagementsystem.shared.exception.ConflictException;
import com.travelmanagementsystem.shared.exception.NotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, jakarta.servlet.http.HttpServletRequest request) {
		log.warn("Not found: {} - {}", ex.getErrorCode(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, jakarta.servlet.http.HttpServletRequest request) {
		log.warn("Conflict: {} - {}", ex.getErrorCode(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, jakarta.servlet.http.HttpServletRequest request) {
		log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, jakarta.servlet.http.HttpServletRequest request) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
			.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
			.collect(Collectors.toList());

		String message = fieldErrors.stream()
			.map(e -> e.field() + ": " + e.message())
			.collect(Collectors.joining(", "));

		log.warn("Validation failed: {}", message);
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of("VALIDATION_ERROR", message, request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, jakarta.servlet.http.HttpServletRequest request) {
		log.warn("Resource not found: {}", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ErrorResponse.of("RESOURCE_NOT_FOUND", "The requested resource was not found", request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnhandled(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
		log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI()));
	}
}
