package com.travelmanagementsystem.travel.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Response after creating or retrieving a travel offering")
public record TravelResponse(
	@Schema(description = "Travel ID", example = "1")
	Long id,

	@Schema(description = "Travel title", example = "Paris Adventure")
	String title,

	@Schema(description = "Travel description", example = "A 7-day tour of Paris")
	String description,

	@Schema(description = "Destination country", example = "France")
	String destinationCountry,

	@Schema(description = "Destination city", example = "Paris")
	String destinationCity,

	@Schema(description = "Start date", example = "2027-06-01")
	LocalDate startDate,

	@Schema(description = "End date", example = "2027-06-07")
	LocalDate endDate,

	@Schema(description = "Duration in days", example = "7")
	Integer durationDays,

	@Schema(description = "Price per person", example = "2500.00")
	BigDecimal price,

	@Schema(description = "Total capacity", example = "20")
	Integer capacity,

	@Schema(description = "Available slots", example = "20")
	Integer availableSlots,

	@Schema(description = "Travel status", example = "DRAFT")
	String status,

	@Schema(description = "Manager user ID who created this travel", example = "1")
	Long managerId,

	@Schema(description = "Planned activities")
	List<ActivityResponse> activities,

	@Schema(description = "Transport arrangements")
	List<TransportResponse> transport,

	@Schema(description = "Creation timestamp", example = "2026-09-20T12:00:00Z")
	Instant createdAt,

	@Schema(description = "Last update timestamp", example = "2026-09-20T12:00:00Z")
	Instant updatedAt
) {

	@Schema(description = "Activity details")
	public record ActivityResponse(
		String name,
		String description,
		Integer dayNumber,
		LocalTime startTime,
		LocalTime endTime
	) {}

	@Schema(description = "Transport details")
	public record TransportResponse(
		String type,
		String provider,
		String departure,
		String arrival,
		Instant departureTime,
		Instant arrivalTime
	) {}
}
