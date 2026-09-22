package com.travelmanagementsystem.identity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.application.JwtService;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import com.travelmanagementsystem.shared.config.RateLimitFilter;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = "rate-limit.login.max-attempts=5")
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";

    private static final String REGISTER_PASSWORD = TestData.generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void resetAndRegisterTestUser() throws Exception {
        rateLimitFilter.resetCounters();
        String body = """
            {"email":"securitytest@example.com","password":"%s"}
            """.formatted(REGISTER_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 201 || status == 409;
            });
    }

    @Nested
    @DisplayName("JWT Authentication")
    class JwtAuthenticationTests {

        @Test
        @DisplayName("Valid token grants access to protected endpoint")
        void validTokenGrantsAccess() throws Exception {
            String token = jwtService.generateAccessToken(Map.of(
                    "userId", 1L,
                    "email", "securitytest@example.com",
                    "roles", Set.of("TRAVELER")));

            mockMvc.perform(get("/api/travel/trips")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 :
                        "Expected non-401 status with valid token, got " + status;
                });
        }

        @Test
        @DisplayName("Expired token returns 401 Unauthorized")
        void expiredTokenReturns401() throws Exception {
            String expiredToken = buildExpiredToken();

            mockMvc.perform(get("/api/travel/trips")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        }

        @Test
        @DisplayName("Tampered token returns 401 Unauthorized")
        void tamperedTokenReturns401() throws Exception {
            String validToken = jwtService.generateAccessToken(Map.of(
                    "userId", 1L,
                    "email", "securitytest@example.com",
                    "roles", Set.of("TRAVELER")));
            String tampered = validToken.substring(0, validToken.length() - 10) + "XXXXXXXXXX";

            mockMvc.perform(get("/api/travel/trips")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }

        @Test
        @DisplayName("No token on protected endpoint returns 401")
        void noTokenReturns401() throws Exception {
            mockMvc.perform(get("/api/travel/trips")
                    .header(API_VERSION_HEADER, API_VERSION))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Malformed Authorization header returns 401")
        void malformedAuthHeaderReturns401() throws Exception {
            mockMvc.perform(get("/api/travel/trips")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "InvalidFormat token123"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Public Route Access")
    class PublicRouteTests {

        @Test
        @DisplayName("Register endpoint is accessible without token")
        void registerAccessibleWithoutToken() throws Exception {
            String body = """
                {"email":"publictest@example.com","password":"%s"}
                """.formatted(REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Login endpoint is accessible without token")
        void loginAccessibleWithoutToken() throws Exception {
            String body = """
                {"email":"securitytest@example.com","password":"%s"}
                """.formatted(REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitTests {

        @Test
        @DisplayName("Login endpoint enforces rate limit")
        void loginEnforcesRateLimit() throws Exception {
            String body = """
                {"email":"ratelimit@example.com","password":"wrongpassword"}
                """;

            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .header(API_VERSION_HEADER, API_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status == 401 || status == 429 :
                            "Expected 401 or 429, got " + status;
                    });
            }

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        }
    }

    private String buildExpiredToken() {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("securitytest@example.com")
                .add("userId", 1L)
                .add("email", "securitytest@example.com")
                .add("roles", Set.of("TRAVELER"))
                .issuedAt(new Date(System.currentTimeMillis() - 2000000))
                .expiration(new Date(System.currentTimeMillis() - 1000000))
                .build();

        return io.jsonwebtoken.Jwts.builder()
                .claims(claims)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
    }
}
