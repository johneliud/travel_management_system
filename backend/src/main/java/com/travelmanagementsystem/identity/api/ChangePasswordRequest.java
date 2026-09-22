package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for changing password")
public record ChangePasswordRequest(
	@Schema(description = "Current password for confirmation", example = "OldPass!123", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	String currentPassword,

	@Schema(description = "New password (min 8 chars, must include uppercase, lowercase, digit, and special character)", example = "NewPass!456", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Size(min = 8, max = 128, message = "must be between 8 and 128 characters")
	@jakarta.validation.constraints.Pattern(
		message = "must contain at least one uppercase letter, one lowercase letter, one digit, and one special character",
		regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$")
	String newPassword,

	@Schema(description = "6-digit OTP from the password change request", example = "654321", requiredMode = Schema.RequiredMode.REQUIRED)
	@jakarta.validation.constraints.NotBlank(message = "is required")
	@jakarta.validation.constraints.Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit code")
	String otp
) {}
