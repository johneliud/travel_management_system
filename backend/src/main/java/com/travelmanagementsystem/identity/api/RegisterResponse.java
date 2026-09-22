package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Response after successful registration")
public record RegisterResponse(
	@Schema(description = "User ID", example = "1")
	Long id,

	@Schema(description = "User email address", example = "user@example.com")
	String email,

	@Schema(description = "Account status", example = "ACTIVE")
	String status,

	@Schema(description = "Account creation timestamp", example = "2026-09-20T12:00:00Z")
	Instant createdAt,

	@Schema(description = "6-digit email verification OTP (non-production only; in production this will be sent via email)", example = "123456")
	String verificationOtp
) {}
