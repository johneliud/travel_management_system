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
@DisplayName("Create Travel Offering (X-API-Version: 1)")
class CreateTravelIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";

    private static final String MANAGER_EMAIL = "travel-manager@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(MANAGER_EMAIL, password)))
            .andExpect(status().isCreated());

        User manager = userRepository.findByEmailWithRoles(MANAGER_EMAIL).orElseThrow();
        manager.getRoles().clear();
        manager.addRole(roleRepository.findByName(Roles.TRAVEL_MANAGER).orElseThrow());
        userRepository.save(manager);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(MANAGER_EMAIL, password)))
            .andExpect(status().isOk())
            .andReturn();

        managerToken = extractField(loginResult.getResponse().getContentAsString(), "accessToken");
    }

    @Test
    @DisplayName("Successful creation returns 201 with DRAFT status")
    void createTravel_success() throws Exception {
        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
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
                            {
                                "name": "Eiffel Tower Visit",
                                "description": "Visit the iconic Eiffel Tower",
                                "dayNumber": 1,
                                "startTime": "09:00",
                                "endTime": "12:00"
                            }
                        ],
                        "transport": [
                            {
                                "type": "FLIGHT",
                                "provider": "Air France",
                                "departure": "London Heathrow",
                                "arrival": "Paris CDG",
                                "departureTime": "%sT08:00:00Z",
                                "arrivalTime": "%sT10:30:00Z"
                            }
                        ]
                    }
                    """.formatted(
                        LocalDate.now().plusDays(30),
                        LocalDate.now().plusDays(36),
                        LocalDate.now().plusDays(30),
                        LocalDate.now().plusDays(30)))
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Paris Adventure"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.activities.length()").value(1))
            .andExpect(jsonPath("$.transport.length()").value(1))
            .andExpect(jsonPath("$.managerId").isNumber())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("End date before start date returns 400")
    void createTravel_endDateBeforeStartDate() throws Exception {
        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Bad Dates",
                        "description": "Invalid date range",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 1,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(5),
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(10)))
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("Duration mismatch returns 400")
    void createTravel_durationMismatch() throws Exception {
        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = LocalDate.now().plusDays(36);

        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Wrong Duration",
                        "description": "Duration does not match date range",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 3,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(start, end, start, start))
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_DURATION"));
    }

    @Test
    @DisplayName("Start date in the past returns 400")
    void createTravel_startDateInPast() throws Exception {
        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Past Travel",
                        "description": "Start date is in the past",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 1,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(
                        LocalDate.now().minusDays(1),
                        LocalDate.now(),
                        LocalDate.now().minusDays(1),
                        LocalDate.now().minusDays(1)))
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("START_DATE_IN_PAST"));
    }

    @Test
    @DisplayName("TRAVELER role is rejected with 403")
    void createTravel_travelerRole_rejected() throws Exception {
        String travelerEmail = "traveler-" + System.nanoTime() + "@example.com";
        String travelerPassword = TestData.generateValidPassword();

        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(travelerEmail, travelerPassword)))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(travelerEmail, travelerPassword)))
            .andExpect(status().isOk())
            .andReturn();

        String travelerToken = extractField(loginResult.getResponse().getContentAsString(), "accessToken");

        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = LocalDate.now().plusDays(36);

        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Unauthorized Travel",
                        "description": "Should be rejected",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(start, end, start, start)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Missing required fields returns 400")
    void createTravel_missingFields() throws Exception {
        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "", "activities": [], "transport": []}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Empty activities list returns 400")
    void createTravel_emptyActivities() throws Exception {
        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = LocalDate.now().plusDays(36);

        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "No Activities",
                        "description": "Missing activities",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(start, end, start, start)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Same-day travel (start = end) with durationDays=1 is valid")
    void createTravel_sameDayTravel() throws Exception {
        LocalDate sameDay = LocalDate.now().plusDays(30);

        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Day Trip",
                        "description": "A single day trip",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 1,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(sameDay, sameDay, sameDay, sameDay)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.durationDays").value(1));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void createTravel_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Manager ID is taken from principal, not request body")
    void createTravel_managerIdFromPrincipal() throws Exception {
        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = LocalDate.now().plusDays(36);

        mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "ID Test",
                        "description": "Manager ID from principal",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 100.00,
                        "capacity": 10,
                        "activities": [
                            {"name": "Walking Tour", "description": "City walk", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "BUS", "provider": "CityBus", "departure": "Hotel", "arrival": "City Center", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT08:30:00Z"}
                        ]
                    }
                    """.formatted(start, end, start, start)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.managerId").isNumber())
            .andExpect(jsonPath("$.managerId").value(getManagerId()));
    }

    private long getManagerId() {
        return userRepository.findByEmailWithRoles(MANAGER_EMAIL)
                .map(User::getId)
                .orElse(0L);
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
