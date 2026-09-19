package com.travelmanagementsystem.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.travelmanagementsystem.shared.exception.BusinessException;
import com.travelmanagementsystem.shared.exception.ConflictException;
import com.travelmanagementsystem.shared.exception.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
		request = mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn("/api/test");
	}

	@Test
	@DisplayName("Should return 404 for NotFoundException")
	void shouldReturn404ForNotFound() {
		NotFoundException ex = NotFoundException.of(String.class, "123");

		ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("STRING_NOT_FOUND");
		assertThat(response.getBody().message()).contains("not found with id: 123");
		assertThat(response.getBody().path()).isEqualTo("/api/test");
		assertThat(response.getBody().timestamp()).isNotNull();
	}

	@Test
	@DisplayName("Should return 409 for ConflictException")
	void shouldReturn409ForConflict() {
		ConflictException ex = ConflictException.of("EMAIL_EXISTS", "Email already registered");

		ResponseEntity<ErrorResponse> response = handler.handleConflict(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("EMAIL_EXISTS");
		assertThat(response.getBody().message()).isEqualTo("Email already registered");
	}

	@Test
	@DisplayName("Should return 400 for BusinessException")
	void shouldReturn400ForBusiness() {
		BusinessException ex = BusinessException.of("INSUFFICIENT_FUNDS", "Not enough balance");

		ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_FUNDS");
		assertThat(response.getBody().message()).isEqualTo("Not enough balance");
	}

	@Test
	@DisplayName("Should return 400 for validation errors with field details")
	void shouldReturn400ForValidation() {
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError1 = new FieldError("request", "email", "must not be blank");
		FieldError fieldError2 = new FieldError("request", "name", "must be at least 2 characters");
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
			(MethodParameter) null, bindingResult);

		ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
		assertThat(response.getBody().errors()).hasSize(2);
		assertThat(response.getBody().errors().get(0).field()).isEqualTo("email");
		assertThat(response.getBody().errors().get(1).field()).isEqualTo("name");
	}

	@Test
	@DisplayName("Should return 500 for unhandled exceptions without stack trace")
	void shouldReturn500ForUnhandled() {
		Exception ex = new RuntimeException("something went wrong");

		ResponseEntity<ErrorResponse> response = handler.handleUnhandled(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
		assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
		assertThat(response.getBody().message()).doesNotContain("something went wrong");
		assertThat(response.getBody().path()).isEqualTo("/api/test");
	}

	@Test
	@DisplayName("Error response should not contain sensitive information")
	void shouldNotContainSensitiveInfo() {
		Exception ex = new RuntimeException("password=secret123");

		ResponseEntity<ErrorResponse> response = handler.handleUnhandled(ex, request);

		assertThat(response.getBody().message()).doesNotContain("secret123");
		assertThat(response.getBody().message()).doesNotContain("password");
	}
}
