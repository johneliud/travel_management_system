package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelmanagementsystem.identity.domain.Role;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.RoleRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
import com.travelmanagementsystem.shared.TestData;
import com.travelmanagementsystem.shared.security.Roles;
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
@DisplayName("Admin User Management (X-API-Version: 1)")
class AdminUserManagementIntegrationTest extends IntegrationTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION = "1";
    private static final String TEST_PASSWORD = TestData.generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.findByEmail("admin_manage@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("target1@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("target2@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("traveler_manage@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("manager_manage@example.com").ifPresent(userRepository::delete);
    }

    private void registerUser(String email) throws Exception {
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

    private String loginAndGetRefreshToken(String email) throws Exception {
        String body = """
            {"email":"%s","password":"%s"}
            """.formatted(email, TEST_PASSWORD);
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

    private void promoteToAdmin(String email) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        Role adminRole = roleRepository.findByName(Roles.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        user.addRole(adminRole);
        userRepository.save(user);
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class ListUsersTests {

        @Test
        @DisplayName("200 OK with paginated results for admin")
        void adminGetsPaginatedUsers() throws Exception {
            String email = "admin_manage@example.com";
            registerUser(email);
            promoteToAdmin(email);
            String token = loginAndGetToken(email);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .param("page", "0")
                    .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("200 OK filters by status")
        void filtersByStatus() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("200 OK filters by email")
        void filtersByEmail() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .param("email", "target1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value(targetEmail));
        }

        @Test
        @DisplayName("200 OK filters by role")
        void filtersByRole() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target2@example.com";
            registerUser(targetEmail);
            User user = userRepository.findByEmailWithRoles(targetEmail).orElseThrow();
            Role adminRole = roleRepository.findByName(Roles.ADMIN).orElseThrow();
            user.addRole(adminRole);
            userRepository.save(user);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.email == '" + targetEmail + "')]").exists());
        }

        @Test
        @DisplayName("403 Forbidden for non-admin")
        void nonAdminGetsForbidden() throws Exception {
            String email = "traveler_manage@example.com";
            registerUser(email);
            String token = loginAndGetToken(email);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("200 OK with user details")
        void adminGetsUserDetails() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();

            mockMvc.perform(get("/api/admin/users/{id}", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(targetEmail))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("404 Not Found for non-existent user")
        void nonExistentUserReturnsNotFound() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            mockMvc.perform(get("/api/admin/users/{id}", 999999L)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        @DisplayName("403 Forbidden for non-admin")
        void nonAdminGetsForbidden() throws Exception {
            String email = "traveler_manage@example.com";
            registerUser(email);
            String token = loginAndGetToken(email);

            mockMvc.perform(get("/api/admin/users/{id}", 1L)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/users/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("204 No Content when suspending user")
        void suspendUser() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();
            String targetToken = loginAndGetToken(targetEmail);

            mockMvc.perform(patch("/api/admin/users/{id}/status", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"SUSPENDED"}
                        """))
                .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("204 No Content when reactivating user")
        void reactivateUser() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String adminToken = loginAndGetToken(adminEmail);

            String targetEmail = "target2@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();

            mockMvc.perform(patch("/api/admin/users/{id}/status", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"SUSPENDED"}
                        """))
                .andExpect(status().isNoContent());

            mockMvc.perform(patch("/api/admin/users/{id}/status", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"ACTIVE"}
                        """))
                .andExpect(status().isNoContent());

            String targetToken = loginAndGetToken(targetEmail);
            mockMvc.perform(get("/api/users/me")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Suspended user refresh tokens are revoked")
        void suspendedUserTokensRevoked() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String adminToken = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();
            String targetRefreshToken = loginAndGetRefreshToken(targetEmail);

            mockMvc.perform(patch("/api/admin/users/{id}/status", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"SUSPENDED"}
                        """))
                .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/auth/refresh")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"refreshToken":"%s"}
                        """.formatted(targetRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("403 Forbidden for non-admin")
        void nonAdminGetsForbidden() throws Exception {
            String email = "traveler_manage@example.com";
            registerUser(email);
            String token = loginAndGetToken(email);

            mockMvc.perform(patch("/api/admin/users/{id}/status", 1L)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"SUSPENDED"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("400 Bad Request with invalid status")
        void invalidStatusReturnsBadRequest() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();

            mockMvc.perform(patch("/api/admin/users/{id}/status", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"INVALID"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/users/{id}/role")
    class UpdateRoleTests {

        @Test
        @DisplayName("204 No Content when updating role")
        void updateRole() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target1@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();

            mockMvc.perform(patch("/api/admin/users/{id}/role", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"role":"TRAVEL_MANAGER"}
                        """))
                .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/admin/users/{id}", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@ == 'TRAVEL_MANAGER')]").exists());
        }

        @Test
        @DisplayName("403 Forbidden for non-admin")
        void nonAdminGetsForbidden() throws Exception {
            String email = "traveler_manage@example.com";
            registerUser(email);
            String token = loginAndGetToken(email);

            mockMvc.perform(patch("/api/admin/users/{id}/role", 1L)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"role":"ADMIN"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("400 Bad Request with invalid role")
        void invalidRoleReturnsBadRequest() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            String targetEmail = "target2@example.com";
            registerUser(targetEmail);
            User target = userRepository.findByEmail(targetEmail).orElseThrow();

            mockMvc.perform(patch("/api/admin/users/{id}/role", target.getId())
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"role":"INVALID_ROLE"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("404 Not Found for non-existent user")
        void nonExistentUserReturnsNotFound() throws Exception {
            String adminEmail = "admin_manage@example.com";
            registerUser(adminEmail);
            promoteToAdmin(adminEmail);
            String token = loginAndGetToken(adminEmail);

            mockMvc.perform(patch("/api/admin/users/{id}/role", 999999L)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"role":"ADMIN"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }
}
