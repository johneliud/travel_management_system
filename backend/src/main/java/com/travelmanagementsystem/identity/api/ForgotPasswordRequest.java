package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for forgot password")
public record ForgotPasswordRequest(
	@Schema(description = "The email address associated with the account", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Email(message = "must be a valid email address")
	String email
) {}
