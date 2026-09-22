package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing a new verification OTP")
public record ResendVerificationResponse(
	@Schema(description = "6-digit verification OTP (non-production only; in production this will be sent via email)", example = "654321")
	String verificationOtp
) {}
