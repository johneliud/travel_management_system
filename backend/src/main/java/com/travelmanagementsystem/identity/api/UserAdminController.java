package com.travelmanagementsystem.identity.api;

import com.travelmanagementsystem.identity.application.UserAdminService;
import com.travelmanagementsystem.shared.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin", headers = "X-API-Version=1")
@Tag(name = "User Administration", description = "Admin-only endpoints for user management")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('" + Roles.ADMIN + "')")
    @Operation(
        summary = "List users with filters and pagination",
        description = "Returns a paginated list of users. Supports filtering by status, role, and email search. Requires ADMIN role. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated user list"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)", content = @Content)
        }
    )
    public ResponseEntity<Page<UserSummaryResponse>> listUsers(
            @Parameter(description = "Filter by status (ACTIVE or SUSPENDED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by email (partial match)")
            @RequestParam(required = false) String email,
            @Parameter(description = "Filter by role name")
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userAdminService.listUsers(status, email, role, pageable));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('" + Roles.ADMIN + "')")
    @Operation(
        summary = "Get user details",
        description = "Returns detailed information about a specific user. Requires ADMIN role. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "User details"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        }
    )
    public ResponseEntity<UserDetailResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getUserById(id));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('" + Roles.ADMIN + "')")
    @Operation(
        summary = "Update user status",
        description = "Activates or suspends a user account. Suspending a user revokes all their active refresh tokens. Requires ADMIN role. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        }
    )
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        userAdminService.updateUserStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('" + Roles.ADMIN + "')")
    @Operation(
        summary = "Update user role",
        description = "Replaces a user's current role with a new one. Requires ADMIN role. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        }
    )
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        userAdminService.updateUserRole(id, request.role());
        return ResponseEntity.noContent().build();
    }
}
