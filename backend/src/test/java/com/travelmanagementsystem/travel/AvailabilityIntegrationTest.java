package com.travelmanagementsystem.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.RoleRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import com.travelmanagementsystem.shared.exception.BusinessException;
import com.travelmanagementsystem.shared.security.Roles;
import com.travelmanagementsystem.travel.application.TravelService;
import com.travelmanagementsystem.travel.domain.Travel;
import com.travelmanagementsystem.travel.domain.TravelStatus;
import com.travelmanagementsystem.travel.infrastructure.persistence.TravelRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@DisplayName("Availability and Capacity Tracking (X-API-Version: 1)")
class AvailabilityIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private TravelService travelService;

    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        travelRepository.deleteAll();
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        // Manager
        String managerEmail = "avail-manager@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Test","lastName":"User","email":"%s","password":"%s"}
                    """.formatted(managerEmail, password)))
            .andExpect(status().isCreated());

        User manager = userRepository.findByEmailWithRoles(managerEmail).orElseThrow();
        manager.getRoles().clear();
        manager.addRole(roleRepository.findByName(Roles.TRAVEL_MANAGER).orElseThrow());
        userRepository.save(manager);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(managerEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        managerToken = extractField(loginResult.getResponse().getContentAsString(), "accessToken");
    }

    private Long createDraftTravel(String token, int capacity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Capacity Test Travel",
                        "description": "Testing capacity tracking",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 2500.00,
                        "capacity": %d,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(
                        LocalDate.now().plusDays(30),
                        LocalDate.now().plusDays(36),
                        capacity,
                        LocalDate.now().plusDays(30),
                        LocalDate.now().plusDays(30)))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        int idStart = body.indexOf("\"id\":") + 5;
        int idEnd = body.indexOf(",", idStart);
        return Long.parseLong(body.substring(idStart, idEnd).trim());
    }

    @Test
    @DisplayName("Created travel has available_slots equal to capacity")
    void createdTravel_slotsMatchCapacity() throws Exception {
        Long travelId = createDraftTravel(managerToken, 10);

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(10, travel.getCapacity());
        assertEquals(10, travel.getAvailableSlots());
        assertFalse(travel.getAvailableSlots() <= 0);
    }

    @Test
    @DisplayName("reserveSlot decrements availableSlots")
    void reserveSlot_success() throws Exception {
        Long travelId = createDraftTravel(managerToken, 5);

        travelService.reserveSlot(travelId);

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(4, travel.getAvailableSlots());
        assertFalse(travel.getAvailableSlots() <= 0);
    }

    @Test
    @DisplayName("reserveSlot fails when no slots available")
    void reserveSlot_noSlotsAvailable() throws Exception {
        Long travelId = createDraftTravel(managerToken, 1);

        travelService.reserveSlot(travelId);

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(0, travel.getAvailableSlots());
        assertTrue(travel.getAvailableSlots() <= 0);

        assertThrows(BusinessException.class, () -> travelService.reserveSlot(travelId));
    }

    @Test
    @DisplayName("releaseSlot increments availableSlots")
    void releaseSlot_success() throws Exception {
        Long travelId = createDraftTravel(managerToken, 5);

        travelService.reserveSlot(travelId);
        travelService.reserveSlot(travelId);

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(3, travel.getAvailableSlots());

        travelService.releaseSlot(travelId);

        travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(4, travel.getAvailableSlots());
    }

    @Test
    @DisplayName("releaseSlot fails when already at capacity")
    void releaseSlot_atCapacity() throws Exception {
        Long travelId = createDraftTravel(managerToken, 5);

        assertThrows(BusinessException.class, () -> travelService.releaseSlot(travelId));
    }

    @Test
    @DisplayName("isFull is true when availableSlots reaches 0")
    void isFull_true() throws Exception {
        Long travelId = createDraftTravel(managerToken, 2);

        travelService.reserveSlot(travelId);
        travelService.reserveSlot(travelId);

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(0, travel.getAvailableSlots());
        assertTrue(travel.getAvailableSlots() <= 0);
    }

    @Test
    @DisplayName("isFull is false when availableSlots > 0")
    void isFull_false() throws Exception {
        Long travelId = createDraftTravel(managerToken, 5);

        travelService.reserveSlot(travelId);
        travelService.reserveSlot(travelId);

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(3, travel.getAvailableSlots());
        assertFalse(travel.getAvailableSlots() <= 0);
    }

    @Test
    @DisplayName("Concurrent reservation: exactly one succeeds when one slot remains")
    void concurrentReservation_lastSlot() throws Exception {
        Long travelId = createDraftTravel(managerToken, 2);

        travelService.reserveSlot(travelId);

        Travel travelBefore = travelRepository.findById(travelId).orElseThrow();
        assertEquals(1, travelBefore.getAvailableSlots());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<Boolean> results = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    travelService.reserveSlot(travelId);
                    synchronized (results) {
                        results.add(true);
                    }
                } catch (BusinessException e) {
                    synchronized (results) {
                        results.add(false);
                    }
                } catch (Exception e) {
                    synchronized (results) {
                        results.add(false);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        long successCount = results.stream().filter(Boolean::booleanValue).count();
        assertEquals(1, successCount, "Exactly one reservation should succeed");
    }

    @Test
    @DisplayName("Concurrent reservations: all succeed when many slots available")
    void concurrentReservation_multipleSlots() throws Exception {
        Long travelId = createDraftTravel(managerToken, 10);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);
        List<Boolean> results = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    travelService.reserveSlot(travelId);
                    synchronized (results) {
                        results.add(true);
                    }
                } catch (Exception e) {
                    synchronized (results) {
                        results.add(false);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        long successCount = results.stream().filter(Boolean::booleanValue).count();
        assertEquals(5, successCount, "All five reservations should succeed");

        Travel travel = travelRepository.findById(travelId).orElseThrow();
        assertEquals(5, travel.getAvailableSlots());
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
