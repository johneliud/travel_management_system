package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for resending email verification OTP")
public record ResendVerificationRequest(
	@Schema(description = "The email address to resend the verification OTP to", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	@Email(message = "must be a valid email address")
	String email
) {}
