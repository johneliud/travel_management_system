package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after successful login")
public record LoginResponse(
	@Schema(description = "JWT access token", example = "eyJ...")
	String accessToken,

	@Schema(description = "JWT refresh token for obtaining new access tokens", example = "eyJ...")
	String refreshToken,

	@Schema(description = "Token type", example = "Bearer")
	String tokenType,

	@Schema(description = "Access token expiry in seconds", example = "900")
	long expiresIn
) {}
