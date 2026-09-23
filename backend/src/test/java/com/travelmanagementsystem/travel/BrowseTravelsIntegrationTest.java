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
@DisplayName("Browse Travels (X-API-Version: 1)")
class BrowseTravelsIntegrationTest extends IntegrationTest {

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

    @BeforeEach
    void setUp() throws Exception {
        travelRepository.deleteAll();
        userRepository.deleteAll();

        String password = TestData.generateValidPassword();

        String managerEmail = "browse-manager@example.com";
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

    private Long createTravel(String token, String title, String country, String city,
                              BigDecimal price, LocalDate startDate, LocalDate endDate,
                              TravelStatus status, String activityName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "%s",
                        "description": "Test travel",
                        "destinationCountry": "%s",
                        "destinationCity": "%s",
                        "startDate": "%s",
                        "endDate": "%s",
                        "durationDays": %d,
                        "price": %s,
                        "capacity": 20,
                        "activities": [
                            {"name": "%s", "description": "Test activity", "dayNumber": 1, "startTime": "09:00", "endTime": "12:00"}
                        ],
                        "transport": [
                            {"type": "FLIGHT", "provider": "Airline", "departure": "Origin", "arrival": "%s", "departureTime": "%sT08:00:00Z", "arrivalTime": "%sT10:00:00Z"}
                        ]
                    }
                    """.formatted(
                        title, country, city, startDate, endDate,
                        java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1,
                        price, activityName, city, startDate, startDate))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        int idStart = body.indexOf("\"id\":") + 5;
        int idEnd = body.indexOf(",", idStart);
        Long travelId = Long.parseLong(body.substring(idStart, idEnd).trim());

        if (status != TravelStatus.DRAFT) {
            Travel travel = travelRepository.findById(travelId).orElseThrow();
            travel.setStatus(status);
            travelRepository.save(travel);
        }

        return travelId;
    }

    @Test
    @DisplayName("Only PUBLISHED travels are returned")
    void browse_onlyPublished() throws Exception {
        createTravel(managerToken, "Draft Travel", "France", "Paris",
                BigDecimal.valueOf(1000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.DRAFT, "Walking Tour");
        createTravel(managerToken, "Published Travel", "France", "Paris",
                BigDecimal.valueOf(2000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Museum Visit");
        createTravel(managerToken, "Cancelled Travel", "France", "Paris",
                BigDecimal.valueOf(1500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.CANCELLED, "City Tour");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].title").value("Published Travel"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Filter by country")
    void browse_filterByCountry() throws Exception {
        createTravel(managerToken, "France Trip", "France", "Paris",
                BigDecimal.valueOf(2000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Eiffel Tower");
        createTravel(managerToken, "Italy Trip", "Italy", "Rome",
                BigDecimal.valueOf(2500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Colosseum");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("country", "France"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].destinationCountry").value("France"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Filter by city")
    void browse_filterByCity() throws Exception {
        createTravel(managerToken, "Paris Trip", "France", "Paris",
                BigDecimal.valueOf(2000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Louvre");
        createTravel(managerToken, "Lyon Trip", "France", "Lyon",
                BigDecimal.valueOf(1500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Old Town");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("city", "Paris"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].destinationCity").value("Paris"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Filter by price range")
    void browse_filterByPrice() throws Exception {
        createTravel(managerToken, "Budget Trip", "France", "Nice",
                BigDecimal.valueOf(500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Beach");
        createTravel(managerToken, "Mid Trip", "France", "Lyon",
                BigDecimal.valueOf(1500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Wine Tour");
        createTravel(managerToken, "Luxury Trip", "France", "Paris",
                BigDecimal.valueOf(5000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Fine Dining");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("minPrice", "1000")
                .param("maxPrice", "2000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].title").value("Mid Trip"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Filter by date range")
    void browse_filterByDateRange() throws Exception {
        createTravel(managerToken, "Summer Trip", "France", "Nice",
                BigDecimal.valueOf(2000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Beach");
        createTravel(managerToken, "Winter Trip", "France", "Chamonix",
                BigDecimal.valueOf(2500), LocalDate.now().plusDays(180), LocalDate.now().plusDays(186),
                TravelStatus.PUBLISHED, "Skiing");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("startDate", LocalDate.now().plusDays(20).toString())
                .param("endDate", LocalDate.now().plusDays(40).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].title").value("Summer Trip"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Filter by activity name")
    void browse_filterByActivity() throws Exception {
        createTravel(managerToken, "Museum Tour", "France", "Paris",
                BigDecimal.valueOf(2000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Louvre Museum Visit");
        createTravel(managerToken, "Beach Trip", "France", "Nice",
                BigDecimal.valueOf(1500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Swimming");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("activity", "museum"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].title").value("Museum Tour"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Pagination works correctly")
    void browse_pagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            createTravel(managerToken, "Travel " + i, "France", "Paris",
                    BigDecimal.valueOf(1000 + i * 100), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                    TravelStatus.PUBLISHED, "Activity " + i);
        }

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("page", "2")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("Sort by price ascending")
    void browse_sortByPriceAsc() throws Exception {
        createTravel(managerToken, "Expensive", "France", "Paris",
                BigDecimal.valueOf(5000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Luxury");
        createTravel(managerToken, "Cheap", "France", "Nice",
                BigDecimal.valueOf(500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Beach");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("sortBy", "price")
                .param("sortDirection", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels[0].title").value("Cheap"))
            .andExpect(jsonPath("$.travels[1].title").value("Expensive"));
    }

    @Test
    @DisplayName("Sort by price descending")
    void browse_sortByPriceDesc() throws Exception {
        createTravel(managerToken, "Expensive", "France", "Paris",
                BigDecimal.valueOf(5000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Luxury");
        createTravel(managerToken, "Cheap", "France", "Nice",
                BigDecimal.valueOf(500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Beach");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("sortBy", "price")
                .param("sortDirection", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels[0].title").value("Expensive"))
            .andExpect(jsonPath("$.travels[1].title").value("Cheap"));
    }

    @Test
    @DisplayName("Empty result set returns correctly")
    void browse_emptyResult() throws Exception {
        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("Combined filters work together")
    void browse_combinedFilters() throws Exception {
        createTravel(managerToken, "France Summer", "France", "Nice",
                BigDecimal.valueOf(1500), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Beach");
        createTravel(managerToken, "France Winter", "France", "Chamonix",
                BigDecimal.valueOf(2500), LocalDate.now().plusDays(180), LocalDate.now().plusDays(186),
                TravelStatus.PUBLISHED, "Skiing");
        createTravel(managerToken, "Italy Summer", "Italy", "Rome",
                BigDecimal.valueOf(2000), LocalDate.now().plusDays(30), LocalDate.now().plusDays(36),
                TravelStatus.PUBLISHED, "Colosseum");

        mockMvc.perform(get("/api/travels")
                .header(API_VERSION_HEADER, API_VERSION)
                .param("country", "France")
                .param("maxPrice", "2000")
                .param("startDate", LocalDate.now().plusDays(20).toString())
                .param("endDate", LocalDate.now().plusDays(40).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.travels.length()").value(1))
            .andExpect(jsonPath("$.travels[0].title").value("France Summer"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
