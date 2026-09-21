package com.travelmanagementsystem.identity.api;

import com.travelmanagementsystem.identity.application.UserAdminService;
import com.travelmanagementsystem.shared.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        summary = "List all users",
        description = "Returns a list of all registered users. Requires ADMIN role. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "User list returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)", content = @Content)
        }
    )
    public ResponseEntity<List<UserSummaryResponse>> listUsers() {
        return ResponseEntity.ok(userAdminService.listUsers());
    }
}
