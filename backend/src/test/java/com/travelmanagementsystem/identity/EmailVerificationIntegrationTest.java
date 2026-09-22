package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.domain.EmailVerificationToken;
import com.travelmanagementsystem.identity.infrastructure.persistence.EmailVerificationTokenRepository;
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
@DisplayName("Email Verification Endpoints (X-API-Version: 1)")
class EmailVerificationIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";
    private static final String TEST_PASSWORD = TestData.generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.findByEmail("verify@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("expired@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("reused@example.com").ifPresent(userRepository::delete);
    }

    private String registerUser(String email) throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(email, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.verificationOtp").isNotEmpty())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int start = responseBody.indexOf("\"verificationOtp\":\"") + 19;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    private String extractJsonField(MvcResult result, String field) throws Exception {
        String body = result.getResponse().getContentAsString();
        int fieldIndex = body.indexOf("\"" + field + "\"");
        int colonIndex = body.indexOf(":", fieldIndex);
        int startQuote = body.indexOf("\"", colonIndex + 1);
        int endQuote = body.indexOf("\"", startQuote + 1);
        return body.substring(startQuote + 1, endQuote);
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

    @Nested
    @DisplayName("POST /api/auth/verify-email")
    class VerifyEmailTests {

        @Test
        @DisplayName("204 No Content when OTP is valid")
        void validOtpSucceeds() throws Exception {
            String email = "verify@example.com";
            String otp = registerUser(email);

            mockMvc.perform(post("/api/auth/verify-email")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"otp":"%s"}
                        """.formatted(otp)))
                .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + loginAndGetToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));
        }

        @Test
        @DisplayName("401 Unauthorized with expired OTP")
        void expiredOtpReturnsUnauthorized() throws Exception {
            String email = "expired@example.com";
            String otp = registerUser(email);

            var user = userRepository.findByEmail(email).orElseThrow();
            String tokenHash = hashOtp(otp);

            tokenRepository.findByTokenHash(tokenHash).ifPresent(tokenRepository::delete);

            EmailVerificationToken expiredToken = new EmailVerificationToken(
                    user, tokenHash, Instant.now().minus(1, ChronoUnit.HOURS),
                    com.travelmanagementsystem.identity.domain.VerificationTokenType.EMAIL_VERIFICATION);
            tokenRepository.save(expiredToken);

            mockMvc.perform(post("/api/auth/verify-email")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"otp":"%s"}
                        """.formatted(otp)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"))
                .andExpect(jsonPath("$.message").value("Verification OTP has expired"));
        }

        @Test
        @DisplayName("401 Unauthorized when OTP is reused")
        void reusedOtpReturnsUnauthorized() throws Exception {
            String email = "reused@example.com";
            String otp = registerUser(email);

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
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"))
                .andExpect(jsonPath("$.message").value("Verification OTP has already been used"));
        }

        @Test
        @DisplayName("401 Unauthorized with non-existent OTP")
        void nonExistentOtpReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/auth/verify-email")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"otp":"000000"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_OTP"))
                .andExpect(jsonPath("$.message").value("Invalid verification OTP"));
        }

        @Test
        @DisplayName("400 Bad Request when OTP is missing")
        void missingOtpReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/verify-email")
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
            mockMvc.perform(post("/api/auth/verify-email")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"otp":"12345"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    private String loginAndGetToken(String email) throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(email, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int start = responseBody.indexOf("\"accessToken\":\"") + 15;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }
}
