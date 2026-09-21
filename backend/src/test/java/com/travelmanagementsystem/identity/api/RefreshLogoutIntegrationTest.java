package com.travelmanagementsystem.identity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.application.JwtService;
import com.travelmanagementsystem.shared.IntegrationTest;

import java.security.SecureRandom;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@DisplayName("Refresh & Logout Endpoints (X-API-Version: 1)")
class RefreshLogoutIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";
    private static final String TEST_EMAIL = "refreshuser@example.com";

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@$!%*?&#";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;

    private static String generateValidPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);

        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        for (int i = 4; i < 16; i++) {
            sb.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }
        return sb.toString();
    }
    
    private static final String TEST_PASSWORD = generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void registerTestUser() throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(TEST_EMAIL, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 201 || status == 409;
            });
    }

    private String loginAndGetRefreshToken() throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int start = responseBody.indexOf("\"refreshToken\":\"") + 16;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("200 OK with new token pair when refresh token is valid")
        void validRefreshTokenReturnsNewPair() throws Exception {
            String oldRefreshToken = loginAndGetRefreshToken();

            String body = """
                {"refreshToken":"%s"}
                """.formatted(oldRefreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
        }

        @Test
        @DisplayName("Old refresh token is revoked after rotation")
        void oldRefreshTokenIsRevokedAfterRotation() throws Exception {
            String oldRefreshToken = loginAndGetRefreshToken();

            String body = """
                {"refreshToken":"%s"}
                """.formatted(oldRefreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("401 Unauthorized with already-revoked refresh token")
        void revokedRefreshTokenReturns401() throws Exception {
            String oldRefreshToken = loginAndGetRefreshToken();

            String body = """
                {"refreshToken":"%s"}
                """.formatted(oldRefreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
        }

        @Test
        @DisplayName("401 Unauthorized with expired refresh token")
        void expiredRefreshTokenReturns401() throws Exception {
            String expiredToken = buildExpiredRefreshToken();

            String body = """
                {"refreshToken":"%s"}
                """.formatted(expiredToken);

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("401 Unauthorized with non-existent refresh token")
        void nonExistentRefreshTokenReturns401() throws Exception {
            String body = """
                {"refreshToken":"this-token-does-not-exist"}
                """;

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("400 Bad Request when refresh token is missing")
        void missingRefreshTokenReturnsBadRequest() throws Exception {
            String body = """
                {}
                """;

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("204 No Content on successful logout")
        void validLogoutReturns204() throws Exception {
            String refreshToken = loginAndGetRefreshToken();

            String body = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

            mockMvc.perform(post("/api/auth/logout")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Refresh token is rejected after logout")
        void logoutThenRefreshFails() throws Exception {
            String refreshToken = loginAndGetRefreshToken();

            String logoutBody = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

            mockMvc.perform(post("/api/auth/logout")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(logoutBody))
                .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(logoutBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
        }

        @Test
        @DisplayName("204 No Content when revoking a non-existent token (idempotent)")
        void logoutNonExistentTokenReturns204() throws Exception {
            String body = """
                {"refreshToken":"non-existent-token"}
                """;

            mockMvc.perform(post("/api/auth/logout")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("204 No Content when revoking an already-revoked token (idempotent)")
        void logoutAlreadyRevokedTokenReturns204() throws Exception {
            String refreshToken = loginAndGetRefreshToken();

            String body = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

            mockMvc.perform(post("/api/auth/logout")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/auth/logout")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("400 Bad Request when refresh token is missing")
        void missingRefreshTokenReturnsBadRequest() throws Exception {
            String body = """
                {}
                """;

            mockMvc.perform(post("/api/auth/logout")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    private String buildExpiredRefreshToken() {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(TEST_EMAIL)
                .add("userId", 1L)
                .add("email", TEST_EMAIL)
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
