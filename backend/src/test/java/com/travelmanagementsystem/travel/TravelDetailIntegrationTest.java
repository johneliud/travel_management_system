package com.travelmanagementsystem.travel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.math.BigDecimal;
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
@DisplayName("Travel Detail (X-API-Version: 1)")
class TravelDetailIntegrationTest extends IntegrationTest {

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
    private Long managerUserId;
    private String otherManagerToken;
    private Long otherManagerUserId;
    private String travelerToken;

    @BeforeEach
    void setUp() throws Exception {
        travelRepository.deleteAll();
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        // Manager 1 (owner)
        String managerEmail = "detail-manager@example.com";
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
        managerUserId = manager.getId();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(managerEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        managerToken = extractField(loginResult.getResponse().getContentAsString(), "accessToken");

        // Manager 2 (non-owner)
        String otherManagerEmail = "detail-other-manager@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Test","lastName":"User","email":"%s","password":"%s"}
                    """.formatted(otherManagerEmail, password)))
            .andExpect(status().isCreated());

        User otherManager = userRepository.findByEmailWithRoles(otherManagerEmail).orElseThrow();
        otherManager.getRoles().clear();
        otherManager.addRole(roleRepository.findByName(Roles.TRAVEL_MANAGER).orElseThrow());
        userRepository.save(otherManager);
        otherManagerUserId = otherManager.getId();

        MvcResult otherLoginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(otherManagerEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        otherManagerToken = extractField(otherLoginResult.getResponse().getContentAsString(), "accessToken");

        // Traveler
        String travelerEmail = "detail-traveler@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Test","lastName":"User","email":"%s","password":"%s"}
                    """.formatted(travelerEmail, password)))
            .andExpect(status().isCreated());

        MvcResult travelerLoginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(travelerEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        travelerToken = extractField(travelerLoginResult.getResponse().getContentAsString(), "accessToken");
    }

    private Long createTravel(String token, String title, TravelStatus status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "%s",
                        "description": "Test travel description",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 2000,
                        "capacity": 20,
                        "activities": [
                            {"name": "Museum Visit", "description": "Louvre tour", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "FLIGHT", "provider": "Airline", "departure": "London", "arrival": "Paris", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT10:00:00Z"}
                        ]
                    }
                    """.formatted(
                        title,
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(30)))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        Long travelId = extractLongId(body);

        if (status != TravelStatus.DRAFT) {
            Travel travel = travelRepository.findById(travelId).orElseThrow();
            travel.setStatus(status);
            travelRepository.save(travel);
        }

        return travelId;
    }

    @Test
    @DisplayName("PUBLISHED travel visible to any authenticated user")
    void publishedTravel_visibleToTraveler() throws Exception {
        Long travelId = createTravel(managerToken, "Paris Adventure", TravelStatus.PUBLISHED);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(travelId))
            .andExpect(jsonPath("$.title").value("Paris Adventure"))
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.activities.length()").value(1))
            .andExpect(jsonPath("$.activities[0].name").value("Museum Visit"))
            .andExpect(jsonPath("$.transport.length()").value(1))
            .andExpect(jsonPath("$.transport[0].type").value("FLIGHT"));
    }

    @Test
    @DisplayName("COMPLETED travel visible to any authenticated user")
    void completedTravel_visibleToTraveler() throws Exception {
        Long travelId = createTravel(managerToken, "Completed Trip", TravelStatus.COMPLETED);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(travelId))
            .andExpect(jsonPath("$.title").value("Completed Trip"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("DRAFT travel not visible to traveler — returns 404")
    void draftTravel_notVisibleToTraveler() throws Exception {
        Long travelId = createTravel(managerToken, "Secret Draft", TravelStatus.DRAFT);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DRAFT travel visible to owning manager")
    void draftTravel_visibleToOwningManager() throws Exception {
        Long travelId = createTravel(managerToken, "Manager Draft", TravelStatus.DRAFT);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(travelId))
            .andExpect(jsonPath("$.title").value("Manager Draft"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("DRAFT travel not visible to non-owning manager — returns 404")
    void draftTravel_notVisibleToOtherManager() throws Exception {
        Long travelId = createTravel(managerToken, "Owner Draft", TravelStatus.DRAFT);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + otherManagerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DRAFT travel visible to admin")
    void draftTravel_visibleToAdmin() throws Exception {
        String password = TestData.generateValidPassword();
        String adminEmail = "detail-admin@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Test","lastName":"User","email":"%s","password":"%s"}
                    """.formatted(adminEmail, password)))
            .andExpect(status().isCreated());

        User admin = userRepository.findByEmailWithRoles(adminEmail).orElseThrow();
        admin.getRoles().clear();
        admin.addRole(roleRepository.findByName(Roles.ADMIN).orElseThrow());
        userRepository.save(admin);

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(adminEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        String adminToken = extractField(adminLoginResult.getResponse().getContentAsString(), "accessToken");

        Long travelId = createTravel(managerToken, "Admin Can See", TravelStatus.DRAFT);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(travelId))
            .andExpect(jsonPath("$.title").value("Admin Can See"));
    }

    @Test
    @DisplayName("CANCELLED travel visible to owning manager")
    void cancelledTravel_visibleToOwningManager() throws Exception {
        Long travelId = createTravel(managerToken, "Cancelled Trip", TravelStatus.CANCELLED);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(travelId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("CANCELLED travel not visible to traveler — returns 404")
    void cancelledTravel_notVisibleToTraveler() throws Exception {
        Long travelId = createTravel(managerToken, "Hidden Cancelled", TravelStatus.CANCELLED);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Non-existent travel returns 404")
    void nonExistentTravel_returns404() throws Exception {
        mockMvc.perform(get("/api/travels/{id}", 99999)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Full detail includes all travel fields")
    void fullDetail_includesAllFields() throws Exception {
        Long travelId = createTravel(managerToken, "Full Detail Trip", TravelStatus.PUBLISHED);

        mockMvc.perform(get("/api/travels/{id}", travelId)
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(travelId))
            .andExpect(jsonPath("$.title").value("Full Detail Trip"))
            .andExpect(jsonPath("$.description").value("Test travel description"))
            .andExpect(jsonPath("$.destinationCountry").value("France"))
            .andExpect(jsonPath("$.destinationCity").value("Paris"))
            .andExpect(jsonPath("$.durationDays").value(7))
            .andExpect(jsonPath("$.price").value(2000))
            .andExpect(jsonPath("$.capacity").value(20))
            .andExpect(jsonPath("$.availableSlots").isNumber())
            .andExpect(jsonPath("$.isFull").isBoolean())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.managerId").value(managerUserId))
            .andExpect(jsonPath("$.activities.length()").value(1))
            .andExpect(jsonPath("$.activities[0].name").value("Museum Visit"))
            .andExpect(jsonPath("$.activities[0].description").value("Louvre tour"))
            .andExpect(jsonPath("$.activities[0].dayNumber").value(1))
            .andExpect(jsonPath("$.transport.length()").value(1))
            .andExpect(jsonPath("$.transport[0].type").value("FLIGHT"))
            .andExpect(jsonPath("$.transport[0].provider").value("Airline"))
            .andExpect(jsonPath("$.transport[0].departure").value("London"))
            .andExpect(jsonPath("$.transport[0].arrival").value("Paris"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    private Long extractLongId(String json) {
        String pattern = "\"id\":";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf(",", start);
        return Long.parseLong(json.substring(start, end).trim());
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
