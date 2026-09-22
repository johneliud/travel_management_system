package com.travelmanagementsystem.travel.application;

import com.travelmanagementsystem.shared.exception.BusinessException;
import com.travelmanagementsystem.travel.api.CreateTravelRequest;
import com.travelmanagementsystem.travel.api.TravelResponse;
import com.travelmanagementsystem.travel.domain.Activity;
import com.travelmanagementsystem.travel.domain.Transport;
import com.travelmanagementsystem.travel.domain.Travel;
import com.travelmanagementsystem.travel.domain.TravelStatus;
import com.travelmanagementsystem.travel.infrastructure.persistence.TravelRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TravelService {

    private final TravelRepository travelRepository;

    public TravelService(TravelRepository travelRepository) {
        this.travelRepository = travelRepository;
    }

    @Transactional
    public TravelResponse create(CreateTravelRequest request, Long managerId) {
        validateDates(request.startDate(), request.endDate());
        validateDuration(request.startDate(), request.endDate(), request.durationDays());

        Travel travel = new Travel(
                request.title(),
                request.description(),
                request.destinationCountry(),
                request.destinationCity(),
                request.startDate(),
                request.endDate(),
                request.durationDays(),
                request.price(),
                request.capacity(),
                managerId);

        travel.setStatus(TravelStatus.DRAFT);

        for (CreateTravelRequest.ActivityRequest a : request.activities()) {
            travel.addActivity(new Activity(a.name(), a.description(), a.dayNumber(), a.startTime(), a.endTime()));
        }

        for (CreateTravelRequest.TransportRequest t : request.transport()) {
            Instant departure = Instant.parse(t.departureTime());
            Instant arrival = Instant.parse(t.arrivalTime());
            travel.addTransport(new Transport(t.type(), t.provider(), t.departure(), t.arrival(), departure, arrival));
        }

        Travel saved = travelRepository.save(travel);
        return toResponse(saved);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate) && !endDate.equals(startDate)) {
            throw BusinessException.of("INVALID_DATE_RANGE", "end date must not be before start date");
        }
        
        if (startDate.isBefore(LocalDate.now())) {
            throw BusinessException.of("START_DATE_IN_PAST", "start date must not be in the past");
        }
    }

    private void validateDuration(LocalDate startDate, LocalDate endDate, Integer durationDays) {
        long actualDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        if (durationDays != actualDays) {
            throw BusinessException.of("INVALID_DURATION",
                    "durationDays (%d) must match the date range (%d days)".formatted(durationDays, actualDays));
        }
    }

    private TravelResponse toResponse(Travel t) {
        return new TravelResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getDestinationCountry(),
                t.getDestinationCity(),
                t.getStartDate(),
                t.getEndDate(),
                t.getDurationDays(),
                t.getPrice(),
                t.getCapacity(),
                t.getAvailableSlots(),
                t.getStatus().name(),
                t.getManagerId(),
                t.getActivities().stream()
                        .map(a -> new TravelResponse.ActivityResponse(a.getName(), a.getDescription(), a.getDayNumber(), a.getStartTime(), a.getEndTime()))
                        .toList(),
                t.getTransport().stream()
                        .map(tr -> new TravelResponse.TransportResponse(tr.getType(), tr.getProvider(), tr.getDeparture(), tr.getArrival(), tr.getDepartureTime(), tr.getArrivalTime()))
                        .toList(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
