package com.travelmanagementsystem.travel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.RoleRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import com.travelmanagementsystem.shared.security.Roles;
import com.travelmanagementsystem.travel.domain.Travel;
import com.travelmanagementsystem.travel.domain.TravelStatus;
import com.travelmanagementsystem.travel.infrastructure.persistence.TravelRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@DisplayName("Complete Travel Offering (X-API-Version: 1)")
class CompleteTravelIntegrationTest extends IntegrationTest {

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

    private String managerToken;
    private String otherManagerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        travelRepository.deleteAll();
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        // Manager (owner)
        String managerEmail = "complete-manager@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
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

        // Other manager (non-owner)
        String otherEmail = "other-complete-manager@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(otherEmail, password)))
            .andExpect(status().isCreated());

        User otherManager = userRepository.findByEmailWithRoles(otherEmail).orElseThrow();
        otherManager.getRoles().clear();
        otherManager.addRole(roleRepository.findByName(Roles.TRAVEL_MANAGER).orElseThrow());
        userRepository.save(otherManager);

        MvcResult otherLogin = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(otherEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        otherManagerToken = extractField(otherLogin.getResponse().getContentAsString(), "accessToken");

        // Admin
        String adminEmail = "admin-complete@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(adminEmail, password)))
            .andExpect(status().isCreated());

        User admin = userRepository.findByEmailWithRoles(adminEmail).orElseThrow();
        admin.getRoles().clear();
        admin.addRole(roleRepository.findByName(Roles.ADMIN).orElseThrow());
        userRepository.save(admin);

        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(adminEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        adminToken = extractField(adminLogin.getResponse().getContentAsString(), "accessToken");
    }

    private Long createDraftTravel(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Paris Adventure",
                        "description": "A 7-day tour of Paris",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 2500.00,
                        "capacity": 20,
                        "activities": [
                            {"name": "Eiffel Tower Visit", "description": "Visit the Eiffel Tower", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "FLIGHT", "provider": "Air France", "departure": "London", "arrival": "Paris", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT10:30:00Z"}
                        ]
                    }
                    """.formatted(
                        LocalDate.now().plusDays(30),
                        LocalDate.now().plusDays(36),
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

    private void setStatus(Long travelId, TravelStatus status) {
        Travel travel = travelRepository.findById(travelId).orElseThrow();
        travel.setStatus(status);
        travelRepository.save(travel);
    }

    @Test
    @DisplayName("Owner completes a PUBLISHED travel returns 200 with COMPLETED status")
    void completeTravel_success() throws Exception {
        Long travelId = createDraftTravel(managerToken);
        setStatus(travelId, TravelStatus.PUBLISHED);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.id").value(travelId));
    }

    @Test
    @DisplayName("Admin can complete any travel")
    void completeTravel_adminOverride() throws Exception {
        Long travelId = createDraftTravel(managerToken);
        setStatus(travelId, TravelStatus.PUBLISHED);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Non-owner manager is rejected with 403")
    void completeTravel_nonOwnerRejected() throws Exception {
        Long travelId = createDraftTravel(managerToken);
        setStatus(travelId, TravelStatus.PUBLISHED);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + otherManagerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Completing a DRAFT travel returns 409")
    void completeTravel_draft() throws Exception {
        Long travelId = createDraftTravel(managerToken);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("Completing a CANCELLED travel returns 409")
    void completeTravel_cancelled() throws Exception {
        Long travelId = createDraftTravel(managerToken);
        setStatus(travelId, TravelStatus.CANCELLED);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("Completing an already COMPLETED travel returns 409")
    void completeTravel_alreadyCompleted() throws Exception {
        Long travelId = createDraftTravel(managerToken);
        setStatus(travelId, TravelStatus.PUBLISHED);
        setStatus(travelId, TravelStatus.COMPLETED);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("Non-existent travel returns 404")
    void completeTravel_notFound() throws Exception {
        mockMvc.perform(post("/api/travels/99999/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TRAVEL_NOT_FOUND"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void completeTravel_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/travels/1/complete")
                .header(API_VERSION_HEADER, API_VERSION))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TRAVELER role is rejected with 403")
    void completeTravel_travelerRejected() throws Exception {
        String travelerEmail = "traveler-complete@example.com";
        String password = TestData.generateValidPassword();

        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(travelerEmail, password)))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(travelerEmail, password)))
            .andExpect(status().isOk())
            .andReturn();

        String travelerToken = extractField(loginResult.getResponse().getContentAsString(), "accessToken");

        Long travelId = createDraftTravel(managerToken);
        setStatus(travelId, TravelStatus.PUBLISHED);

        mockMvc.perform(post("/api/travels/" + travelId + "/complete")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isForbidden());
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
