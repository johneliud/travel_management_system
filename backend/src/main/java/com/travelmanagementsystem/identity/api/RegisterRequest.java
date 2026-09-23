package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for user registration")
public record RegisterRequest(
	@Schema(description = "First name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Size(min = 1, max = 100, message = "must be between 1 and 100 characters")
	String firstName,

	@Schema(description = "Last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Size(min = 1, max = 100, message = "must be between 1 and 100 characters")
	String lastName,

	@Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.Email(message = "must be a valid email address")
	@jakarta.validation.constraints.NotBlank(message = "is required")
	String email,

	@Schema(description = "Password (min 8 chars, must include uppercase, lowercase, digit, and special character)", example = "Str0ng!Pass", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Size(min = 8, max = 128, message = "must be between 8 and 128 characters")
	@jakarta.validation.constraints.Pattern(
		message = "must contain at least one uppercase letter, one lowercase letter, one digit, and one special character",
		regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$")
	String password
) {}
