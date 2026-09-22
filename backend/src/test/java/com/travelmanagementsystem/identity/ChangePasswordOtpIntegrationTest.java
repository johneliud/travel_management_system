package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@DisplayName("Change Password OTP (X-API-Version: 1)")
class ChangePasswordOtpIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";
    private static final String TEST_PASSWORD = TestData.generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanUp() {
    }

    private String registerAndGetToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, TEST_PASSWORD)))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, TEST_PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
        return extractField(loginResult.getResponse().getContentAsString(), "accessToken");
    }

    @Test
    @DisplayName("Request OTP returns 200 with verificationOtp")
    void requestOtpReturnsOtp() throws Exception {
        String token = registerAndGetToken("otp-request@example.com");

        mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verificationOtp").isNotEmpty());
    }

    @Test
    @DisplayName("401 Unauthorized without token on request-otp")
    void requestOtpWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Password change succeeds with valid OTP")
    void changePasswordWithValidOtp() throws Exception {
        String token = registerAndGetToken("otp-valid@example.com");
        String newPassword = TestData.generateValidPassword();

        MvcResult otpResult = mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        String otp = extractField(otpResult.getResponse().getContentAsString(), "verificationOtp");

        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"%s"}
                    """.formatted(TEST_PASSWORD, newPassword, otp)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted("otp-valid@example.com", newPassword)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("401 with invalid OTP")
    void changePasswordWithInvalidOtp() throws Exception {
        String token = registerAndGetToken("otp-invalid@example.com");

        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"000000"}
                    """.formatted(TEST_PASSWORD, TestData.generateValidPassword())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"));
    }

    @Test
    @DisplayName("401 with already-used OTP")
    void changePasswordWithUsedOtp() throws Exception {
        String token = registerAndGetToken("otp-used@example.com");
        String newPassword = TestData.generateValidPassword();

        MvcResult otpResult = mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        String otp = extractField(otpResult.getResponse().getContentAsString(), "verificationOtp");

        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"%s"}
                    """.formatted(TEST_PASSWORD, newPassword, otp)))
            .andExpect(status().isNoContent());

        String anotherNewPassword = TestData.generateValidPassword();
        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"%s"}
                    """.formatted(newPassword, anotherNewPassword, otp)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"));
    }

    @Test
    @DisplayName("New request-otp invalidates previous unused OTP")
    void newRequestInvalidatesOldOtp() throws Exception {
        String token = registerAndGetToken("otp-invalidate@example.com");

        MvcResult firstOtpResult = mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        String firstOtp = extractField(firstOtpResult.getResponse().getContentAsString(), "verificationOtp");

        MvcResult secondOtpResult = mockMvc.perform(post("/api/auth/change-password/request-otp")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        String secondOtp = extractField(secondOtpResult.getResponse().getContentAsString(), "verificationOtp");

        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"%s"}
                    """.formatted(TEST_PASSWORD, TestData.generateValidPassword(), firstOtp)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"));

        String newPassword = TestData.generateValidPassword();
        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s","otp":"%s"}
                    """.formatted(TEST_PASSWORD, newPassword, secondOtp)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("400 with missing otp field")
    void changePasswordWithoutOtp() throws Exception {
        String token = registerAndGetToken("otp-missing@example.com");

        mockMvc.perform(post("/api/auth/change-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s"}
                    """.formatted(TEST_PASSWORD, TestData.generateValidPassword())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
