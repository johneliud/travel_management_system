package com.travelmanagementsystem.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;

import com.travelmanagementsystem.identity.domain.Role;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.RoleRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.IntegrationTest;
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
@DisplayName("Role-Based Authorization Enforcement")
class RoleAuthorizationIntegrationTest extends IntegrationTest {

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

    private static final String TEST_PASSWORD = generateValidPassword();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.findByEmail("traveler@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("admin@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("manager@example.com").ifPresent(userRepository::delete);
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

    private void promoteToAdmin(String email) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        Role adminRole = roleRepository.findByName(Roles.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        user.addRole(adminRole);
        userRepository.save(user);
    }

    private void assignRole(String email, String roleName) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(roleName + " role not found"));
        user.addRole(role);
        userRepository.save(user);
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class ListUsersTests {

        @Test
        @DisplayName("403 Forbidden for TRAVELER role")
        void travelerGetsForbidden() throws Exception {
            String email = "traveler@example.com";
            registerUser(email);
            String token = loginAndGetToken(email, TEST_PASSWORD);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("200 OK for ADMIN role")
        void adminGetsUserList() throws Exception {
            String email = "admin@example.com";
            registerUser(email);
            promoteToAdmin(email);
            String token = loginAndGetToken(email, TEST_PASSWORD);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.email == 'admin@example.com')].roles[?(@ == 'ADMIN')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'admin@example.com')].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("401 Unauthorized without token")
        void unauthenticatedGetsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 Forbidden for TRAVEL_MANAGER role")
        void travelManagerGetsForbidden() throws Exception {
            String email = "manager@example.com";
            registerUser(email);
            assignRole(email, Roles.TRAVEL_MANAGER);

            String token = loginAndGetToken(email, TEST_PASSWORD);

            mockMvc.perform(get("/api/admin/users")
                    .header(API_VERSION_HEADER, API_VERSION)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }
}
