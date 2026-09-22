package com.travelmanagementsystem.travel.application;

import com.travelmanagementsystem.shared.exception.BusinessException;
import com.travelmanagementsystem.shared.exception.ConflictException;
import com.travelmanagementsystem.shared.exception.NotFoundException;
import com.travelmanagementsystem.shared.security.Roles;
import com.travelmanagementsystem.travel.api.CreateTravelRequest;
import com.travelmanagementsystem.travel.api.TravelResponse;
import com.travelmanagementsystem.travel.api.UpdateTravelRequest;
import com.travelmanagementsystem.travel.domain.Activity;
import com.travelmanagementsystem.travel.domain.Transport;
import com.travelmanagementsystem.travel.domain.Travel;
import com.travelmanagementsystem.travel.domain.TravelStatus;
import com.travelmanagementsystem.travel.infrastructure.persistence.TravelRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.security.access.AccessDeniedException;
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

    @Transactional
    public TravelResponse updateTravel(Long travelId, UpdateTravelRequest request, Long callerId, String callerRole) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> NotFoundException.of(Travel.class, travelId));

        validateOwnership(travel, callerId, callerRole);

        boolean isPublished = travel.getStatus() == TravelStatus.PUBLISHED;

        if (isPublished) {
            validatePublishedFieldUpdate(request);
        }

        if (request.title() != null) {
            travel.setTitle(request.title());
        }
        
        if (request.description() != null) {
            travel.setDescription(request.description());
        }
        
        if (request.destinationCountry() != null) {
            travel.setDestinationCountry(request.destinationCountry());
        }
        
        if (request.destinationCity() != null) {
            travel.setDestinationCity(request.destinationCity());
        }
        
        if (request.startDate() != null) {
            travel.setStartDate(request.startDate());
        }
        
        if (request.endDate() != null) {
            travel.setEndDate(request.endDate());
        }
        
        if (request.durationDays() != null) {
            travel.setDurationDays(request.durationDays());
        }
        
        if (request.price() != null) {
            travel.setPrice(request.price());
        }
        
        if (request.capacity() != null) {
            travel.setCapacity(request.capacity());
        }

        if (request.activities() != null) {
            travel.getActivities().clear();
            
            for (UpdateTravelRequest.ActivityRequest a : request.activities()) {
                travel.addActivity(new Activity(a.name(), a.description(), a.dayNumber(), a.startTime(), a.endTime()));
            }
        }

        if (request.transport() != null) {
            travel.getTransport().clear();
            
            for (UpdateTravelRequest.TransportRequest t : request.transport()) {
                Instant departure = Instant.parse(t.departureTime());
                Instant arrival = Instant.parse(t.arrivalTime());
                travel.addTransport(new Transport(t.type(), t.provider(), t.departure(), t.arrival(), departure, arrival));
            }
        }

        if (!isPublished) {
            validateDates(travel.getStartDate(), travel.getEndDate());
            validateDuration(travel.getStartDate(), travel.getEndDate(), travel.getDurationDays());
        }

        Travel saved = travelRepository.save(travel);
        return toResponse(saved);
    }

    @Transactional
    public TravelResponse publish(Long travelId, Long callerId, String callerRole) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> NotFoundException.of(Travel.class, travelId));

        validateOwnership(travel, callerId, callerRole);

        if (travel.getStatus() != TravelStatus.DRAFT) {
            throw ConflictException.of("INVALID_STATUS_TRANSITION",
                    "cannot publish a travel in %s status; only DRAFT travels can be published".formatted(travel.getStatus()));
        }

        validateCompleteness(travel);

        travel.setStatus(TravelStatus.PUBLISHED);

        Travel saved = travelRepository.save(travel);
        return toResponse(saved);
    }

    @Transactional
    public TravelResponse cancel(Long travelId, Long callerId, String callerRole) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> NotFoundException.of(Travel.class, travelId));

        validateOwnership(travel, callerId, callerRole);

        if (travel.getStatus() == TravelStatus.COMPLETED) {
            throw ConflictException.of("INVALID_STATUS_TRANSITION",
                    "cannot cancel a travel in COMPLETED status");
        }

        if (travel.getStatus() == TravelStatus.CANCELLED) {
            throw ConflictException.of("INVALID_STATUS_TRANSITION",
                    "travel is already cancelled");
        }

        travel.setStatus(TravelStatus.CANCELLED);

        Travel saved = travelRepository.save(travel);
        return toResponse(saved);
    }

    private void validateCompleteness(Travel travel) {
        if (travel.getTitle() == null || travel.getTitle().isBlank()) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "title is required for publication");
        }
        
        if (travel.getDescription() == null || travel.getDescription().isBlank()) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "description is required for publication");
        }
        
        if (travel.getDestinationCountry() == null || travel.getDestinationCountry().isBlank()) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "destinationCountry is required for publication");
        }
        
        if (travel.getDestinationCity() == null || travel.getDestinationCity().isBlank()) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "destinationCity is required for publication");
        }
        
        if (travel.getStartDate() == null) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "startDate is required for publication");
        }
        
        if (travel.getEndDate() == null) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "endDate is required for publication");
        }
        
        if (travel.getDurationDays() == null) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "durationDays is required for publication");
        }
        
        if (travel.getPrice() == null) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "price is required for publication");
        }
        
        if (travel.getCapacity() == null) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "capacity is required for publication");
        }
        
        if (travel.getActivities() == null || travel.getActivities().isEmpty()) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "at least one activity is required for publication");
        }
        
        if (travel.getTransport() == null || travel.getTransport().isEmpty()) {
            throw BusinessException.of("INCOMPLETE_TRAVEL", "at least one transport arrangement is required for publication");
        }
    }

    private void validateOwnership(Travel travel, Long callerId, String callerRole) {
        if (Roles.ADMIN.equals(callerRole)) {
            return;
        }
        
        if (!travel.getManagerId().equals(callerId)) {
            throw new AccessDeniedException("you can only edit your own travel offerings");
        }
    }

    private void validatePublishedFieldUpdate(UpdateTravelRequest request) {
        if (request.title() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "title cannot be changed after publication");
        }
        
        if (request.destinationCountry() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "destinationCountry cannot be changed after publication");
        }
        
        if (request.destinationCity() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "destinationCity cannot be changed after publication");
        }
        
        if (request.startDate() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "startDate cannot be changed after publication");
        }
        
        if (request.endDate() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "endDate cannot be changed after publication");
        }
        
        if (request.durationDays() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "durationDays cannot be changed after publication");
        }
        
        if (request.price() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "price cannot be changed after publication");
        }
        
        if (request.capacity() != null) {
            throw ConflictException.of("PUBLISHED_FIELD_LOCKED",
                    "capacity cannot be changed after publication");
        }
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
