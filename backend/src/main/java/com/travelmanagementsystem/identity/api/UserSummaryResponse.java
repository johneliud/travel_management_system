package com.travelmanagementsystem.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

@Schema(description = "Summary of a user for admin views")
public record UserSummaryResponse(
	@Schema(description = "User ID", example = "1")
	Long id,

	@Schema(description = "User email address", example = "user@example.com")
	String email,

	@Schema(description = "Account status", example = "ACTIVE")
	String status,

	@Schema(description = "Assigned role names", example = "[\"TRAVELER\"]")
	Set<String> roles,

	@Schema(description = "Account creation timestamp", example = "2026-09-20T12:00:00Z")
	Instant createdAt
) {}
