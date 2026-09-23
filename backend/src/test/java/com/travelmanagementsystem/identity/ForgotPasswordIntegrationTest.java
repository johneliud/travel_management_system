package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.domain.EmailVerificationToken;
import com.travelmanagementsystem.identity.domain.VerificationTokenType;
import com.travelmanagementsystem.identity.infrastructure.persistence.EmailVerificationTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.RefreshTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@DisplayName("Forgot Password Endpoints (X-API-Version: 1)")
class ForgotPasswordIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";
    private static final String TEST_PASSWORD = TestData.generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.findByEmail("forgot@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("reset@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("expired-reset@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("wrong-reset@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("nonexistent@example.com").ifPresent(userRepository::delete);
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Test","lastName":"User","email":"%s","password":"%s"}
                    """.formatted(email, TEST_PASSWORD)))
            .andExpect(status().isCreated());
    }

    private String requestForgotPassword(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/forgot-password")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("If this email is registered, a reset code has been sent."))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        int otpIndex = body.indexOf("\"verificationOtp\":\"");
        if (otpIndex == -1) {
            return null;
        }
        int start = otpIndex + 19;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int start = responseBody.indexOf("\"accessToken\":\"") + 15;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("200 OK with OTP for existing email")
        void existingEmailReturnsOtp() throws Exception {
            String email = "forgot@example.com";
            registerUser(email);

            String otp = requestForgotPassword(email);
            if (otp != null) {
                org.junit.jupiter.api.Assertions.assertEquals(6, otp.length());
            }
        }

        @Test
        @DisplayName("200 OK with identical response for non-existent email")
        void nonExidentEmailReturnsGenericResponse() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"nonexistent@example.com"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email is registered, a reset code has been sent."))
                .andExpect(jsonPath("$.verificationOtp").doesNotExist());
        }

        @Test
        @DisplayName("400 Bad Request when email is missing")
        void missingEmailReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when email format is invalid")
        void invalidEmailFormatReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"not-an-email"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("204 No Content when OTP is valid and password is updated")
        void validResetSucceeds() throws Exception {
            String email = "reset@example.com";
            registerUser(email);
            String otp = requestForgotPassword(email);

            mockMvc.perform(post("/api/auth/reset-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","otp":"%s","newPassword":"%s"}
                        """.formatted(email, otp, TEST_PASSWORD + "New1!")))
                .andExpect(status().isNoContent());

            String newPass = TEST_PASSWORD + "New1!";
            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, newPass)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("401 Unauthorized with expired OTP")
        void expiredOtpReturnsUnauthorized() throws Exception {
            String email = "expired-reset@example.com";
            registerUser(email);
            String otp = requestForgotPassword(email);

            var user = userRepository.findByEmail(email).orElseThrow();
            String tokenHash = hashOtp(otp);
            tokenRepository.findByTokenHash(tokenHash).ifPresent(tokenRepository::delete);

            EmailVerificationToken expiredToken = new EmailVerificationToken(
                    user, tokenHash, Instant.now().minus(1, ChronoUnit.HOURS),
                    VerificationTokenType.PASSWORD_RESET);
            tokenRepository.save(expiredToken);

            mockMvc.perform(post("/api/auth/reset-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","otp":"%s","newPassword":"%s"}
                        """.formatted(email, otp, TEST_PASSWORD + "New1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"))
                .andExpect(jsonPath("$.message").value("Verification OTP has expired"));
        }

        @Test
        @DisplayName("401 Unauthorized with incorrect OTP")
        void incorrectOtpReturnsUnauthorized() throws Exception {
            String email = "wrong-reset@example.com";
            registerUser(email);

            mockMvc.perform(post("/api/auth/reset-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","otp":"000000","newPassword":"%s"}
                        """.formatted(email, TEST_PASSWORD + "New1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"))
                .andExpect(jsonPath("$.message").value("Invalid verification OTP"));
        }

        @Test
        @DisplayName("400 Bad Request when fields are missing")
        void missingFieldsReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/reset-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when OTP format is invalid")
        void invalidOtpFormatReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/reset-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@example.com","otp":"12345","newPassword":"NewPass!123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when password does not meet requirements")
        void weakPasswordReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/reset-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@example.com","otp":"123456","newPassword":"weak"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
