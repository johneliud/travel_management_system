package com.travelmanagementsystem.travel.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Partial update request for a travel offering. Only non-null fields are updated.")
public record UpdateTravelRequest(

	@Schema(description = "Travel title", example = "Paris Adventure")
	@Size(max = 255, message = "must be at most 255 characters")
	String title,

	@Schema(description = "Travel description", example = "A 7-day tour of Paris")
	String description,

	@Schema(description = "Destination country", example = "France")
	@Size(max = 100, message = "must be at most 100 characters")
	String destinationCountry,

	@Schema(description = "Destination city", example = "Paris")
	@Size(max = 100, message = "must be at most 100 characters")
	String destinationCity,

	@Schema(description = "Start date", example = "2027-06-01")
	LocalDate startDate,

	@Schema(description = "End date", example = "2027-06-07")
	LocalDate endDate,

	@Schema(description = "Duration in days", example = "7")
	@Min(value = 1, message = "must be at least 1")
	Integer durationDays,

	@Schema(description = "Price per person", example = "2500.00")
	@DecimalMin(value = "0.00", message = "must not be negative")
	BigDecimal price,

	@Schema(description = "Total capacity (max participants)", example = "20")
	@Min(value = 1, message = "must be at least 1")
	Integer capacity,

	@Schema(description = "Replace the activities list (if provided, replaces all existing activities)")
	@Valid
	List<ActivityRequest> activities,

	@Schema(description = "Replace the transport list (if provided, replaces all existing transport)")
	@Valid
	List<TransportRequest> transport
) {

	@Schema(description = "An activity within the travel offering")
	public record ActivityRequest(

		@Schema(description = "Activity name", example = "Eiffel Tower Visit", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "is required")
		@Size(max = 255, message = "must be at most 255 characters")
		String name,

		@Schema(description = "Activity description", example = "Visit the iconic Eiffel Tower", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "is required")
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
		@NotNull(message = "is required")
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
