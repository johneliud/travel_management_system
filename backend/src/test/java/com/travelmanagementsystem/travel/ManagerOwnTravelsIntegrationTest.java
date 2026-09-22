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
@DisplayName("Manager Own Travels (X-API-Version: 1)")
class ManagerOwnTravelsIntegrationTest extends IntegrationTest {

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

    private String managerAToken;
    private String managerBToken;

    @BeforeEach
    void setUp() throws Exception {
        travelRepository.deleteAll();
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        // Manager A
        String managerAEmail = "mine-manager-a@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(managerAEmail, password)))
            .andExpect(status().isCreated());

        User managerA = userRepository.findByEmailWithRoles(managerAEmail).orElseThrow();
        managerA.getRoles().clear();
        managerA.addRole(roleRepository.findByName(Roles.TRAVEL_MANAGER).orElseThrow());
        userRepository.save(managerA);

        MvcResult loginA = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(managerAEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        managerAToken = extractField(loginA.getResponse().getContentAsString(), "accessToken");

        // Manager B
        String managerBEmail = "mine-manager-b@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(managerBEmail, password)))
            .andExpect(status().isCreated());

        User managerB = userRepository.findByEmailWithRoles(managerBEmail).orElseThrow();
        managerB.getRoles().clear();
        managerB.addRole(roleRepository.findByName(Roles.TRAVEL_MANAGER).orElseThrow());
        userRepository.save(managerB);

        MvcResult loginB = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(managerBEmail, password)))
            .andExpect(status().isOk())
            .andReturn();
        managerBToken = extractField(loginB.getResponse().getContentAsString(), "accessToken");
    }

    private Long createTravel(String token, String title, TravelStatus status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "%s",
                        "description": "Test travel",
                        "destinationCountry": "France",
                        "destinationCity": "Paris",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": 7,
                        "price": 2000,
                        "capacity": 20,
                        "activities": [
                            {"name": "Tour", "description": "City tour", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
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
            var travel = travelRepository.findById(travelId).orElseThrow();
            travel.setStatus(status);
            travelRepository.save(travel);
        }

        return travelId;
    }

    @Test
    @DisplayName("Manager sees all their own travels across all statuses")
    void managerSeesOwnTravels_allStatuses() throws Exception {
        createTravel(managerAToken, "Manager A Draft", TravelStatus.DRAFT);
        createTravel(managerAToken, "Manager A Published", TravelStatus.PUBLISHED);
        createTravel(managerAToken, "Manager A Completed", TravelStatus.COMPLETED);
        createTravel(managerAToken, "Manager A Cancelled", TravelStatus.CANCELLED);

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(4))
            .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("Manager A does not see Manager B's travels")
    void managerDoesNotSeeOtherManagerTravels() throws Exception {
        createTravel(managerAToken, "Manager A Trip", TravelStatus.PUBLISHED);
        createTravel(managerAToken, "Manager A Draft", TravelStatus.DRAFT);
        createTravel(managerBToken, "Manager B Trip", TravelStatus.PUBLISHED);

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Traveler cannot access /mine — returns 403")
    void travelerCannotAccessMine() throws Exception {
        String password = TestData.generateValidPassword();
        String travelerEmail = "mine-traveler@example.com";
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

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + travelerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Pagination works with multiple travels")
    void paginationWorks() throws Exception {
        for (int i = 0; i < 5; i++) {
            createTravel(managerAToken, "Trip " + i, TravelStatus.DRAFT);
        }

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerAToken)
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerAToken)
                .param("page", "2")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    @DisplayName("Empty list returns correctly for new manager")
    void emptyListReturnsCorrectly() throws Exception {
        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Results sorted by createdAt descending")
    void resultsSortedByCreatedAtDesc() throws Exception {
        createTravel(managerAToken, "First Trip", TravelStatus.DRAFT);
        Thread.sleep(50);
        createTravel(managerAToken, "Second Trip", TravelStatus.DRAFT);
        Thread.sleep(50);
        createTravel(managerAToken, "Third Trip", TravelStatus.DRAFT);

        mockMvc.perform(get("/api/travels/mine")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels[0].title").value("Third Trip"))
            .andExpect(jsonPath("$.travels[1].title").value("Second Trip"))
            .andExpect(jsonPath("$.travels[2].title").value("First Trip"));
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
