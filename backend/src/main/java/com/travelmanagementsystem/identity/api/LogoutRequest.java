package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for logout")
public record LogoutRequest(
	@Schema(description = "JWT refresh token to revoke", example = "eyJ...")
	@jakarta.validation.constraints.NotBlank(message = "is required")
	String refreshToken
) {}
