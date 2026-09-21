package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for token refresh")
public record RefreshRequest(
	@Schema(description = "JWT refresh token to exchange for a new token pair", example = "eyJ...")
	@jakarta.validation.constraints.NotBlank(message = "is required")
	String refreshToken
) {}
