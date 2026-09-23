package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@DisplayName("Full Auth Lifecycle (X-API-Version: 1)")
class FullAuthLifecycleIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Complete lifecycle: register → verify → login → access → refresh → change-password → old-refresh-revoked → logout")
    void fullLifecycle() throws Exception {
        String email = "lifecycle@example.com";
        String originalPassword = TestData.generateValidPassword();
        String newPassword = TestData.generateValidPassword();

        // Register
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Test","lastName":"User","email":"%s","password":"%s"}
                    """.formatted(email, originalPassword)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.verificationOtp").isNotEmpty())
            .andReturn();

        String otp = extractField(registerResult.getResponse().getContentAsString(), "verificationOtp");

        // Verify email
        mockMvc.perform(post("/api/auth/verify-email")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"otp":"%s"}
                    """.formatted(otp)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/verify-email")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"otp":"%s"}
                    """.formatted(otp)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"));

        // Login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, originalPassword)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        String accessToken = extractField(loginBody, "accessToken");
        String refreshToken = extractField(loginBody, "refreshToken");

        // Access protected route
        mockMvc.perform(get("/api/users/me")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.emailVerified").value(true))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Refresh token
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"%s"}
                    """.formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        String refreshBody = refreshResult.getResponse().getContentAsString();
        String newAccessToken = extractField(refreshBody, "accessToken");
        String newRefreshToken = extractField(refreshBody, "refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"%s"}
                    """.formatted(refreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // Change password — request OTP first
        MvcResult otpResult = mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk())
            .andReturn();
        String changePwOtp = extractField(otpResult.getResponse().getContentAsString(), "verificationOtp");

        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + newAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"%s"}
                    """.formatted(originalPassword, newPassword, changePwOtp)))
            .andExpect(status().isNoContent());

        // Old refresh token revoked by password change
        mockMvc.perform(post("/api/auth/refresh")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"%s"}
                    """.formatted(newRefreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, originalPassword)))
            .andExpect(status().isUnauthorized());

        // Login with new password
        MvcResult newLoginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, newPassword)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        String newLoginBody = newLoginResult.getResponse().getContentAsString();
        String finalAccessToken = extractField(newLoginBody, "accessToken");
        String finalRefreshToken = extractField(newLoginBody, "refreshToken");

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"%s"}
                    """.formatted(finalRefreshToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"%s"}
                    """.formatted(finalRefreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // Access route still works with remaining access token
        mockMvc.perform(get("/api/users/me")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + finalAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
