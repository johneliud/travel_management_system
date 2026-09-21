package com.travelmanagementsystem.identity.api;

import com.travelmanagementsystem.identity.application.AuthService;
import com.travelmanagementsystem.identity.application.RegistrationService;
import com.travelmanagementsystem.identity.application.UserProfileService;
import com.travelmanagementsystem.shared.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", headers = "X-API-Version=1")
@Tag(name = "Authentication", description = "Registration, login, token refresh, logout, and password change endpoints")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final UserProfileService userProfileService;

    public AuthController(
            RegistrationService registrationService,
            AuthService authService,
            UserProfileService userProfileService) {
        this.registrationService = registrationService;
        this.authService = authService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register a new account",
        description = "Creates a new TRAVELER account. Requires header X-API-Version: 1. Returns 409 if the email is already registered.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
        }
    )
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate and receive tokens",
        description = "Validates credentials and returns an access/refresh token pair. Requires header X-API-Version: 1. Returns 401 for invalid credentials and 403 for disabled accounts.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @ApiResponse(responseCode = "403", description = "Account disabled", content = @Content)
        }
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Exchange a refresh token for a new token pair",
        description = "Rotates the refresh token: the old refresh token is revoked and a new access/refresh token pair is issued. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token refresh successful"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token", content = @Content)
        }
    )
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Revoke a refresh token",
        description = "Revokes the presented refresh token so it can no longer be used. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
        }
    )
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @Operation(
        summary = "Change own password",
        description = "Changes the authenticated user's password after verifying the current password. Revokes all existing refresh tokens to force re-login on other sessions. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect", content = @Content)
        }
    )
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
