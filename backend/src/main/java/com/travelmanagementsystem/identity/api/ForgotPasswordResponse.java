package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after requesting a password reset")
public record ForgotPasswordResponse(
	@Schema(description = "Generic message confirming the request was processed", example = "If this email is registered, a reset code has been sent.")
	String message,

	@Schema(description = "6-digit password reset OTP (non-production only; in production this will be sent via email)", example = "654321")
	String verificationOtp
) {
	public static ForgotPasswordResponse generic() {
		return new ForgotPasswordResponse(
			"If this email is registered, a reset code has been sent.",
			null);
	}

	public static ForgotPasswordResponse withOtp(String otp) {
		return new ForgotPasswordResponse(
			"If this email is registered, a reset code has been sent.",
			otp);
	}
}
