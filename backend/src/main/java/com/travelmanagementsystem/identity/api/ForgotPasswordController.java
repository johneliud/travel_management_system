package com.travelmanagementsystem.identity.api;

import com.travelmanagementsystem.identity.application.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", headers = "X-API-Version=1")
@Tag(name = "Password Reset", description = "Forgot password and password reset endpoints")
public class ForgotPasswordController {

    private final EmailVerificationService emailVerificationService;

    public ForgotPasswordController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request a password reset code",
        description = "Generates a 6-digit OTP for password reset. Returns a generic response regardless of whether the email exists, to prevent account enumeration. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Request processed (generic response)"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
        }
    )
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = emailVerificationService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password with OTP",
        description = "Resets the user's password using the OTP from /forgot-password. Revokes all existing refresh tokens to force re-login on other sessions. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or already-used OTP", content = @Content)
        }
    )
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        emailVerificationService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
