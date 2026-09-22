package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for email verification")
public record VerificationRequest(
	@Schema(description = "6-digit verification OTP", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit code")
	String otp
) {}
