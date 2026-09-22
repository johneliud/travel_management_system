package com.travelmanagementsystem.travel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
@DisplayName("Update Travel Offering (X-API-Version: 1)")
class UpdateTravelIntegrationTest extends IntegrationTest {

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
    private Long managerId;
    private Long otherManagerId;

    @BeforeEach
    void setUp() throws Exception {
        travelRepository.deleteAll();
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        // Manager (owner)
        String managerEmail = "owner-manager@example.com";
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
        managerId = manager.getId();

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
        String otherEmail = "other-manager@example.com";
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
        otherManagerId = otherManager.getId();

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
        String adminEmail = "admin-update@example.com";
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
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        int idStart = body.indexOf("\"id\":") + 5;
        int idEnd = body.indexOf(",", idStart);
        return Long.parseLong(body.substring(idStart, idEnd).trim());
    }

    private Long createPublishedTravel(String token) throws Exception {
        Long travelId = createDraftTravel(token);
        Travel travel = travelRepository.findById(travelId).orElseThrow();
        travel.setStatus(TravelStatus.PUBLISHED);
        travelRepository.save(travel);
        return travelId;
    }

    @Test
    @DisplayName("Owner updates description on DRAFT travel returns 200")
    void updateTravel_ownerEditSuccess() throws Exception {
        Long travelId = createDraftTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "Updated description for Paris Adventure"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Updated description for Paris Adventure"))
            .andExpect(jsonPath("$.title").value("Paris Adventure"));
    }

    @Test
    @DisplayName("Owner updates multiple fields on DRAFT travel")
    void updateTravel_ownerMultiFieldUpdate() throws Exception {
        Long travelId = createDraftTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Updated Paris Trip",
                        "price": 3000.00,
                        "capacity": 25
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Paris Trip"))
            .andExpect(jsonPath("$.price").value(3000.00))
            .andExpect(jsonPath("$.capacity").value(25));
    }

    @Test
    @DisplayName("Owner replaces activities list")
    void updateTravel_replaceActivities() throws Exception {
        Long travelId = createDraftTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "activities": [
                            {"name": "Louvre Museum", "description": "Visit the Louvre", "dayNumber": 1, "startTime": "10:00", "endTime": "16:00"},
                            {"name": "Seine Cruise", "description": "Evening cruise", "dayNumber": 1, "startTime": "18:00", "endTime": "20:00"}
                        ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activities.length()").value(2))
            .andExpect(jsonPath("$.activities[0].name").value("Louvre Museum"))
            .andExpect(jsonPath("$.activities[1].name").value("Seine Cruise"));
    }

    @Test
    @DisplayName("Non-owner manager is rejected with 403")
    void updateTravel_nonOwnerRejected() throws Exception {
        Long travelId = createDraftTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + otherManagerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "Unauthorized edit"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Admin can edit any travel")
    void updateTravel_adminOverride() throws Exception {
        Long travelId = createDraftTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "Admin updated description"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Admin updated description"));
    }

    @Test
    @DisplayName("Updating price on PUBLISHED travel returns 409")
    void updateTravel_publishedPriceLocked() throws Exception {
        Long travelId = createPublishedTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"price": 9999.00}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PUBLISHED_FIELD_LOCKED"));
    }

    @Test
    @DisplayName("Updating title on PUBLISHED travel returns 409")
    void updateTravel_publishedTitleLocked() throws Exception {
        Long travelId = createPublishedTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "New Title"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PUBLISHED_FIELD_LOCKED"));
    }

    @Test
    @DisplayName("Updating capacity on PUBLISHED travel returns 409")
    void updateTravel_publishedCapacityLocked() throws Exception {
        Long travelId = createPublishedTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"capacity": 50}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PUBLISHED_FIELD_LOCKED"));
    }

    @Test
    @DisplayName("Updating dates on PUBLISHED travel returns 409")
    void updateTravel_publishedDatesLocked() throws Exception {
        Long travelId = createPublishedTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\": \"" + LocalDate.now().plusDays(60) + "\", \"endDate\": \"" + LocalDate.now().plusDays(66) + "\", \"durationDays\": 7}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PUBLISHED_FIELD_LOCKED"));
    }

    @Test
    @DisplayName("Admin can update description on PUBLISHED travel")
    void updateTravel_adminCanEditPublished() throws Exception {
        Long travelId = createPublishedTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "Admin updated published travel"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Admin updated published travel"));
    }

    @Test
    @DisplayName("Admin cannot change locked fields on PUBLISHED travel")
    void updateTravel_adminCannotChangeLockedFields() throws Exception {
        Long travelId = createPublishedTravel(managerToken);

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"price": 9999.00}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PUBLISHED_FIELD_LOCKED"));
    }

    @Test
    @DisplayName("Non-existent travel returns 404")
    void updateTravel_notFound() throws Exception {
        mockMvc.perform(patch("/api/travels/99999")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "Does not exist"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TRAVEL_NOT_FOUND"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void updateTravel_unauthenticated() throws Exception {
        mockMvc.perform(patch("/api/travels/1")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TRAVELER role is rejected with 403")
    void updateTravel_travelerRejected() throws Exception {
        String travelerEmail = "traveler-update@example.com";
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

        mockMvc.perform(patch("/api/travels/" + travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "Unauthorized"}
                    """))
            .andExpect(status().isForbidden());
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
