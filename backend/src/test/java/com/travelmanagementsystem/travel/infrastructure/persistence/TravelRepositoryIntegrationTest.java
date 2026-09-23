package com.travelmanagementsystem.travel.infrastructure.persistence;

import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.travel.domain.Activity;
import com.travelmanagementsystem.travel.domain.Transport;
import com.travelmanagementsystem.travel.domain.Travel;
import com.travelmanagementsystem.travel.domain.TravelStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Travel Repository Integration Tests")
class TravelRepositoryIntegrationTest extends IntegrationTest {

    @Autowired
    private TravelRepository travelRepository;

    @AfterEach
    void cleanUp() {
        travelRepository.deleteAll();
    }

    private Travel createTestTravel(String title, Long managerId) {
        return new Travel(
                title,
                "A wonderful trip",
                "France",
                "Paris",
                LocalDate.of(2027, 6, 1),
                LocalDate.of(2027, 6, 7),
                7,
                new BigDecimal("2500.00"),
                20,
                managerId);
    }

    @Test
    void roundTripWithActivitiesAndTransport() {
        Travel travel = createTestTravel("Paris Adventure", 1L);

        travel.addActivity(new Activity(
                "Eiffel Tower Visit",
                "Visit the iconic tower",
                1,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)));

        travel.addActivity(new Activity(
                "Seine River Cruise",
                "Evening cruise on the Seine",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0)));

        travel.addTransport(new Transport(
                "FLIGHT",
                "Air France",
                "London Heathrow",
                "Paris CDG",
                Instant.parse("2027-06-01T08:00:00Z"),
                Instant.parse("2027-06-01T10:30:00Z")));

        Travel saved = travelRepository.save(travel);

        Travel found = travelRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Paris Adventure", found.getTitle());
        assertEquals("France", found.getDestinationCountry());
        assertEquals("Paris", found.getDestinationCity());
        assertEquals(TravelStatus.DRAFT, found.getStatus());
        assertEquals(1L, found.getManagerId());
        assertEquals(20, found.getCapacity());
        assertEquals(20, found.getAvailableSlots());

        List<Activity> activities = found.getActivities();
        assertEquals(2, activities.size());
        assertEquals("Eiffel Tower Visit", activities.get(0).getName());
        assertEquals("Seine River Cruise", activities.get(1).getName());

        List<Transport> transports = found.getTransport();
        assertEquals(1, transports.size());
        assertEquals("FLIGHT", transports.get(0).getType());
        assertEquals("Air France", transports.get(0).getProvider());
    }

    @Test
    void updateStatus() {
        Travel travel = createTestTravel("Status Test", 1L);
        Travel saved = travelRepository.save(travel);

        saved.setStatus(TravelStatus.PUBLISHED);
        travelRepository.save(saved);

        Travel found = travelRepository.findById(saved.getId()).orElseThrow();
        assertEquals(TravelStatus.PUBLISHED, found.getStatus());
    }

    @Test
    void updateAvailableSlots() {
        Travel travel = createTestTravel("Slots Test", 1L);
        Travel saved = travelRepository.save(travel);

        saved.setAvailableSlots(15);
        travelRepository.save(saved);

        Travel found = travelRepository.findById(saved.getId()).orElseThrow();
        assertEquals(15, found.getAvailableSlots());
    }

    @Test
    void findByManagerId() {
        Travel travel1 = createTestTravel("Manager 1 Trip", 1L);
        Travel travel2 = createTestTravel("Manager 1 Trip 2", 1L);
        Travel travel3 = createTestTravel("Manager 2 Trip", 2L);
        travelRepository.save(travel1);
        travelRepository.save(travel2);
        travelRepository.save(travel3);

        List<Travel> results = travelRepository.findByManagerId(1L);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> t.getManagerId().equals(1L)));
    }

    @Test
    void findByStatus() {
        Travel draft = createTestTravel("Draft Trip", 1L);
        draft.setStatus(TravelStatus.DRAFT);
        Travel published = createTestTravel("Published Trip", 1L);
        published.setStatus(TravelStatus.PUBLISHED);
        travelRepository.save(draft);
        travelRepository.save(published);

        List<Travel> drafts = travelRepository.findByStatus(TravelStatus.DRAFT);
        assertEquals(1, drafts.size());
        assertEquals("Draft Trip", drafts.get(0).getTitle());
    }

    @Test
    void findAvailable() {
        Travel available = createTestTravel("Available Trip", 1L);
        available.setStatus(TravelStatus.PUBLISHED);
        available.setAvailableSlots(10);
        Travel full = createTestTravel("Full Trip", 1L);
        full.setStatus(TravelStatus.PUBLISHED);
        full.setAvailableSlots(0);
        Travel cancelled = createTestTravel("Cancelled Trip", 1L);
        cancelled.setStatus(TravelStatus.CANCELLED);
        cancelled.setAvailableSlots(5);
        travelRepository.save(available);
        travelRepository.save(full);
        travelRepository.save(cancelled);

        List<Travel> results = travelRepository.findAvailable();
        assertEquals(1, results.size());
        assertEquals("Available Trip", results.get(0).getTitle());
    }

    @Test
    void timestampsAreSet() {
        Travel travel = createTestTravel("Timestamp Test", 1L);
        Travel saved = travelRepository.save(travel);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void removeActivity() {
        Travel travel = createTestTravel("Remove Activity Test", 1L);
        Activity activity = new Activity("Test Activity", "desc", 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        travel.addActivity(activity);
        Travel saved = travelRepository.save(travel);

        Travel found = travelRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, found.getActivities().size());

        found.removeActivity(found.getActivities().get(0));
        travelRepository.save(found);

        Travel updated = travelRepository.findById(saved.getId()).orElseThrow();
        assertTrue(updated.getActivities().isEmpty());
    }
}
