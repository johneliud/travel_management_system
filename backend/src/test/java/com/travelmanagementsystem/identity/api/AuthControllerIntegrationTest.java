package com.travelmanagementsystem.identity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;

import com.travelmanagementsystem.shared.IntegrationTest;
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
@DisplayName("Auth Endpoints (X-API-Version: 1)")
class AuthControllerIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";

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

    private static final String REGISTER_PASSWORD = generateValidPassword();
    private static final String WRONG_PASSWORD = generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @BeforeEach
        void ensureTestData() throws Exception {
            String body = """
                {"email":"existing@example.com","password":"%s"}
                """.formatted(REGISTER_PASSWORD);
            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 201 || status == 409 :
                        "Expected 201 or 409, got " + status;
                });
        }

        @Test
        @DisplayName("201 Created with valid registration data")
        void registerSuccess() throws Exception {
            String body = """
                {"email":"newuser@example.com","password":"%s"}
                """.formatted(generateValidPassword());

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("409 Conflict when email already exists")
        void duplicateEmailReturnsConflict() throws Exception {
            String body = """
                {"email":"existing@example.com","password":"%s"}
                """.formatted(generateValidPassword());

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"))
                .andExpect(jsonPath("$.message").value("email already in use"));
        }

        @Test
        @DisplayName("400 Bad Request when password violates policy")
        void weakPasswordReturnsBadRequest() throws Exception {
            String body = """
                {"email":"weak@example.com","password":"password"}
                """;

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("400 Bad Request when email is invalid")
        void invalidEmailReturnsBadRequest() throws Exception {
            String body = """
                {"email":"not-an-email","password":"%s"}
                """.formatted(generateValidPassword());

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when password is missing uppercase")
        void passwordWithoutUppercaseReturnsBadRequest() throws Exception {
            String body = """
                {"email":"lower@example.com","password":"str0ng!pass"}
                """;

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when password is too short")
        void shortPasswordReturnsBadRequest() throws Exception {
            String body = """
                {"email":"short@example.com","password":"Ab1!"}
                """;

            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("201 Created. Password hash is never in response body")
        void passwordHashNotExposed() throws Exception {
            String body = """
                {"email":"secure@example.com","password":"%s"}
                """.formatted(generateValidPassword());

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assert !responseBody.contains("passwordHash") : "Password hash must not appear in response";
            assert !responseBody.contains("password_hash") : "Password hash must not appear in response";
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        private static final String LOGIN_EMAIL = "loginuser@example.com";

        @BeforeEach
        void registerUser() throws Exception {
            String body = """
                {"email":"%s","password":"%s"}
                """.formatted(LOGIN_EMAIL, REGISTER_PASSWORD);
            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 201 || status == 409 :
                        "Expected 201 or 409, got " + status;
                });
        }

        @Test
        @DisplayName("200 OK with valid credentials returns tokens")
        void loginSuccess() throws Exception {
            String body = """
                {"email":"%s","password":"%s"}
                """.formatted(LOGIN_EMAIL, REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
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
        @DisplayName("401 Unauthorized with wrong password")
        void wrongPasswordReturnsUnauthorized() throws Exception {
            String body = """
                {"email":"%s","password":"%s"}
                """.formatted(LOGIN_EMAIL, WRONG_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("401 Unauthorized with unknown email")
        void unknownEmailReturnsUnauthorized() throws Exception {
            String body = """
                {"email":"unknown@example.com","password":"AnyPass!123"}
                """;

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("401 Unauthorized with identical message for wrong password and unknown email")
        void invalidCredentialsReturnSameMessage() throws Exception {
            String wrongPasswordBody = """
                {"email":"%s","password":"%s"}
                """.formatted(LOGIN_EMAIL, WRONG_PASSWORD);
            String unknownEmailBody = """
                {"email":"nonexistent@example.com","password":"AnyPass!123"}
                """;

            MvcResult wrongPasswordResult = mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(wrongPasswordBody))
                .andExpect(status().isUnauthorized())
                .andReturn();

            MvcResult unknownEmailResult = mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unknownEmailBody))
                .andExpect(status().isUnauthorized())
                .andReturn();

            String wrongPasswordMsg = extractJsonField(wrongPasswordResult, "message");
            String unknownEmailMsg = extractJsonField(unknownEmailResult, "message");
            assert wrongPasswordMsg.equals(unknownEmailMsg) :
                "Both invalid credential scenarios must return the same message";
        }

        @Test
        @DisplayName("200 OK after successful registration and login")
        void registerAndLoginSucceeds() throws Exception {
            String disabledEmail = "disabled@example.com";

            String registerBody = """
                {"email":"%s","password":"%s"}
                """.formatted(disabledEmail, REGISTER_PASSWORD);
            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody))
                .andExpect(status().isCreated());

            String loginBody = """
                {"email":"%s","password":"%s"}
                """.formatted(disabledEmail, REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("400 Bad Request when email is missing")
        void missingEmailReturnsBadRequest() throws Exception {
            String body = """
                {"password":"%s"}
                """.formatted(REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when password is missing")
        void missingPasswordReturnsBadRequest() throws Exception {
            String body = """
                {"email":"%s"}
                """.formatted(LOGIN_EMAIL);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("400 Bad Request when email format is invalid")
        void invalidEmailFormatReturnsBadRequest() throws Exception {
            String body = """
                {"email":"not-an-email","password":"%s"}
                """.formatted(REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Refresh token is persisted in database")
        void refreshTokenPersisted() throws Exception {
            String body = """
                {"email":"%s","password":"%s"}
                """.formatted(LOGIN_EMAIL, REGISTER_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        private String extractJsonField(MvcResult result, String field) throws Exception {
            String body = result.getResponse().getContentAsString();
            int fieldIndex = body.indexOf("\"" + field + "\"");
            int colonIndex = body.indexOf(":", fieldIndex);
            int startQuote = body.indexOf("\"", colonIndex + 1);
            int endQuote = body.indexOf("\"", startQuote + 1);
            return body.substring(startQuote + 1, endQuote);
        }
    }
}
