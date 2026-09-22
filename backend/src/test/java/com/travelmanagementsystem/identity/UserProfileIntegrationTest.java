package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
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
@DisplayName("User Profile Endpoints (X-API-Version: 1)")
class UserProfileIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";
    private static final String TEST_PASSWORD = TestData.generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        String[] emails = {
            "profileuser@example.com", "changepw@example.com",
            "wrongpw@example.com", "invalidpw@example.com",
            "revoketokens@example.com", "updatepw@example.com",
            "dupupdate@example.com"
        };
        for (String email : emails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
    }

    private String registerUser(String email) throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(email, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                .header(API_VERSION_HEADER, API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 201 || status == 409;
            });
        return loginAndGetToken(email, TEST_PASSWORD);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
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

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
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
    @DisplayName("GET /api/users/me")
    class GetProfileTests {

        @Test
        @DisplayName("200 OK with own profile")
        void authenticatedUserGetsProfile() throws Exception {
            String email = "profileuser@example.com";
            String token = registerUser(email);

            mockMvc.perform(get("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[?(@ == 'TRAVELER')]").exists())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("401 Unauthorized without token")
        void unauthenticatedGetsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me")
    class UpdateProfileTests {

        @Test
        @DisplayName("200 OK with updated email")
        void updateEmail() throws Exception {
            String email = "profileuser@example.com";
            String token = registerUser(email);
            String newEmail = "updated_profileuser@example.com";

            mockMvc.perform(patch("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s"}
                        """.formatted(newEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.emailVerified").value(false));
        }

        @Test
        @DisplayName("409 Conflict when email is already in use")
        void duplicateEmailReturnsConflict() throws Exception {
            String email = "profileuser@example.com";
            String token = registerUser(email);

            String otherEmail = "dupupdate@example.com";
            String registerBody = """
                {"email":"%s","password":"%s"}
                """.formatted(otherEmail, TEST_PASSWORD);
            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 201 || status == 409;
                });

            mockMvc.perform(patch("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s"}
                        """.formatted(otherEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
        }

        @Test
        @DisplayName("401 Unauthorized without token")
        void unauthenticatedGetsUnauthorized() throws Exception {
            mockMvc.perform(patch("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"new@example.com"}
                        """))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 OK with null email (no change)")
        void nullEmailNoChange() throws Exception {
            String email = "profileuser@example.com";
            String token = registerUser(email);

            mockMvc.perform(patch("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("204 No Content with correct current password")
        void correctCurrentPasswordSucceeds() throws Exception {
            String email = "changepw@example.com";
            String token = registerUser(email);
            String newPassword = TestData.generateValidPassword();

            mockMvc.perform(post("/api/auth/change-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"currentPassword":"%s","newPassword":"%s"}
                        """.formatted(TEST_PASSWORD, newPassword)))
                .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("401 Unauthorized with wrong current password")
        void wrongCurrentPasswordFails() throws Exception {
            String email = "wrongpw@example.com";
            String token = registerUser(email);

            mockMvc.perform(post("/api/auth/change-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"currentPassword":"%s","newPassword":"%s"}
                        """.formatted(TestData.generateValidPassword(), TestData.generateValidPassword())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

            mockMvc.perform(post("/api/auth/login")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, TestData.generateValidPassword())))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refresh tokens are revoked after password change")
        void refreshTokensRevokedAfterPasswordChange() throws Exception {
            String email = "revoketokens@example.com";
            mockMvc.perform(post("/api/auth/register")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, TEST_PASSWORD)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 201 || status == 409;
                });

            String refreshToken = loginAndGetRefreshToken(email, TEST_PASSWORD);

            String token = loginAndGetToken(email, TEST_PASSWORD);
            String newPassword = TestData.generateValidPassword();
            mockMvc.perform(post("/api/auth/change-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"currentPassword":"%s","newPassword":"%s"}
                        """.formatted(TEST_PASSWORD, newPassword)))
                .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("400 Bad Request with invalid new password")
        void invalidNewPasswordFails() throws Exception {
            String email = "invalidpw@example.com";
            String token = registerUser(email);

            mockMvc.perform(post("/api/auth/change-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"currentPassword":"%s","newPassword":"weak"}
                        """.formatted(TEST_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("401 Unauthorized without token")
        void unauthenticatedGetsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/auth/change-password")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"currentPassword":"%s","newPassword":"%s"}
                        """.formatted(TestData.generateValidPassword(), TestData.generateValidPassword())))
                .andExpect(status().isUnauthorized());
        }
    }
}
