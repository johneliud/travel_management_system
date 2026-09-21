package com.travelmanagementsystem.identity.api;

import com.travelmanagementsystem.identity.application.UserProfileService;
import com.travelmanagementsystem.shared.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/users", headers = "X-API-Version=1")
@Tag(name = "User Profile", description = "Authenticated user self-service endpoints")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get own profile",
        description = "Returns the authenticated user's profile. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
        }
    )
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(userProfileService.getProfile(principal.getUserId()));
    }

    @PatchMapping("/me")
    @Operation(
        summary = "Update own profile",
        description = "Updates the authenticated user's profile. Currently supports email change (triggers re-verification). Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
        }
    )
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(principal.getUserId(), request));
    }
}
