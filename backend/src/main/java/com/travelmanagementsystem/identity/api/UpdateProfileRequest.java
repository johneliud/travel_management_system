package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for updating own profile")
public record UpdateProfileRequest(
	@Schema(description = "New email address (triggers re-verification)", example = "newemail@example.com")
	@jakarta.validation.constraints.Email(message = "must be a valid email address")
	String email
) {}
