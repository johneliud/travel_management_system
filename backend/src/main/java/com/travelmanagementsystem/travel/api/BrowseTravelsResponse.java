package com.travelmanagementsystem.travel.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated response for browsing published travels")
public record BrowseTravelsResponse(
	@Schema(description = "List of travel offerings")
	List<TravelResponse> travels,

	@Schema(description = "Current page number (0-indexed)", example = "0")
	int page,

	@Schema(description = "Page size", example = "20")
	int size,

	@Schema(description = "Total number of matching travels", example = "42")
	long totalElements,

	@Schema(description = "Total number of pages", example = "3")
	int totalPages
) {}
