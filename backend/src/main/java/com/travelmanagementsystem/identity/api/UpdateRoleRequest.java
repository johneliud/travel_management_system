package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for updating user role")
public record UpdateRoleRequest(
	@Schema(description = "New role name", example = "TRAVEL_MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	@Pattern(regexp = "^(ADMIN|TRAVEL_MANAGER|TRAVELER)$", message = "must be ADMIN, TRAVEL_MANAGER, or TRAVELER")
	String role
) {}
