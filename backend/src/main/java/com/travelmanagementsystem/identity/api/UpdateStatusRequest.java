package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for updating user status")
public record UpdateStatusRequest(
	@Schema(description = "New account status", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	@Pattern(regexp = "^(ACTIVE|SUSPENDED)$", message = "must be ACTIVE or SUSPENDED")
	String status
) {}
