package com.travelmanagementsystem.travel.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Request body for creating a new travel offering")
public record CreateTravelRequest(

	@Schema(description = "Travel title", example = "Paris Adventure", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	@Size(max = 255, message = "must be at most 255 characters")
	String title,

	@Schema(description = "Travel description", example = "A 7-day tour of Paris", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	String description,

	@Schema(description = "Destination country", example = "France", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	@Size(max = 100, message = "must be at most 100 characters")
	String destinationCountry,

	@Schema(description = "Destination city", example = "Paris", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "is required")
	@Size(max = 100, message = "must be at most 100 characters")
	String destinationCity,

	@Schema(description = "Start date (must not be in the past)", example = "2027-06-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "is required")
	LocalDate startDate,

	@Schema(description = "End date (must be on or after start date)", example = "2027-06-07", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "is required")
	LocalDate endDate,

	@Schema(description = "Duration in days (must match date range)", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "is required")
	@Min(value = 1, message = "must be at least 1")
	Integer durationDays,

	@Schema(description = "Price per person", example = "2500.00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "is required")
	@DecimalMin(value = "0.00", message = "must not be negative")
	BigDecimal price,

	@Schema(description = "Total capacity (max participants)", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "is required")
	@Min(value = 1, message = "must be at least 1")
	Integer capacity,

	@Schema(description = "Planned activities", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "must contain at least one activity")
	@Valid
	List<ActivityRequest> activities,

	@Schema(description = "Transport arrangements", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "must contain at least one transport arrangement")
	@Valid
	List<TransportRequest> transport
) {

	@Schema(description = "An activity within the travel offering")
	public record ActivityRequest(

		@Schema(description = "Activity name", example = "Eiffel Tower Visit", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "is required")
		@Size(max = 255, message = "must be at most 255 characters")
		String name,

		@Schema(description = "Activity description", example = "Visit the iconic Eiffel Tower", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "is required")
		String description,

		@Schema(description = "Day number (1-indexed)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "is required")
		@Min(value = 1, message = "must be at least 1")
		Integer dayNumber,

		@Schema(description = "Start time", example = "09:00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "is required")
		LocalTime startTime,

		@Schema(description = "End time", example = "12:00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "is required")
		LocalTime endTime
	) {}

	@Schema(description = "A transport arrangement within the travel offering")
	public record TransportRequest(

		@Schema(description = "Transport type (FLIGHT, BUS, TRAIN, etc.)", example = "FLIGHT", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "is required")
		@Size(max = 50, message = "must be at most 50 characters")
		String type,

		@Schema(description = "Transport provider", example = "Air France")
		String provider,

		@Schema(description = "Departure location", example = "London Heathrow")
		String departure,

		@Schema(description = "Arrival location", example = "Paris CDG")
		String arrival,

		@Schema(description = "Departure time (ISO-8601)", example = "2027-06-01T08:00:00Z")
		String departureTime,

		@Schema(description = "Arrival time (ISO-8601)", example = "2027-06-01T10:30:00Z")
		String arrivalTime
	) {}
}
