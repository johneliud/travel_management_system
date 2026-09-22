package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing a password change OTP")
public record ChangePasswordOtpResponse(
	@Schema(description = "6-digit OTP for password change (non-production only; in production this will be sent via email)", example = "654321")
	String verificationOtp
) {}
