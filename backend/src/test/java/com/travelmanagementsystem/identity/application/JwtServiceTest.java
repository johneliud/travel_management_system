package com.travelmanagementsystem.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-service-unit-tests-32b";
    private static final long ACCESS_TOKEN_MS = 900000;
    private static final long REFRESH_TOKEN_MS = 604800000;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, ACCESS_TOKEN_MS, REFRESH_TOKEN_MS);
    }

    @Test
    @DisplayName("generates and parses a valid access token")
    void generateAndParseAccessToken() {
        Map<String, Object> claims = Map.of(
                "userId", 1L,
                "email", "user@example.com",
                "roles", java.util.Set.of("TRAVELER"));

        String token = jwtService.generateAccessToken(claims);

        Claims parsed = jwtService.parseToken(token);
        assertThat(parsed.get("userId", Long.class)).isEqualTo(1L);
        assertThat(parsed.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(parsed.get("roles")).isNotNull();
    }

    @Test
    @DisplayName("generates and parses a valid refresh token")
    void generateAndParseRefreshToken() {
        Map<String, Object> claims = Map.of(
                "userId", 2L,
                "email", "refresh@example.com",
                "roles", java.util.Set.of("ADMIN"));

        String token = jwtService.generateRefreshToken(claims);

        Claims parsed = jwtService.parseToken(token);
        assertThat(parsed.get("userId", Long.class)).isEqualTo(2L);
        assertThat(parsed.get("email", String.class)).isEqualTo("refresh@example.com");
    }

    @Test
    @DisplayName("token has correct expiration")
    void tokenHasCorrectExpiration() {
        Map<String, Object> claims = Map.of("userId", 1L);

        String token = jwtService.generateAccessToken(claims);
        Claims parsed = jwtService.parseToken(token);

        long tokenLifetime = parsed.getExpiration().getTime() - parsed.getIssuedAt().getTime();
        assertThat(tokenLifetime).isEqualTo(ACCESS_TOKEN_MS);
    }

    @Test
    @DisplayName("isValid returns true for a valid token")
    void isValidReturnsTrueForValidToken() {
        Map<String, Object> claims = Map.of("userId", 1L);
        String token = jwtService.generateAccessToken(claims);

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid returns false for a tampered token")
    void isValidReturnsFalseForTamperedToken() {
        Map<String, Object> claims = Map.of("userId", 1L);
        String token = jwtService.generateAccessToken(claims);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for an empty string")
    void isValidReturnsFalseForEmptyString() {
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for a random string")
    void isValidReturnsFalseForRandomString() {
        assertThat(jwtService.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("returns configured access token expiration")
    void returnsConfiguredExpiration() {
        assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(ACCESS_TOKEN_MS);
    }
}
