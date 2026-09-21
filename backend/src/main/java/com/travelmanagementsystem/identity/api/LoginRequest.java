package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for login")
public record LoginRequest(
	@Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.Email(message = "must be a valid email address")
	@jakarta.validation.constraints.NotBlank(message = "is required")
	String email,

	@Schema(description = "Password", example = "Str0ng!Pass", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	String password
) {}
