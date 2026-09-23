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
@Tag(name = "Email Verification", description = "Email verification endpoints")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/verify-email")
    @Operation(
        summary = "Verify email address with OTP",
        description = "Verifies the user's email address using a 6-digit OTP. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or already-used OTP", content = @Content)
        }
    )
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerificationRequest request) {
        emailVerificationService.verify(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @Operation(
        summary = "Resend email verification OTP",
        description = "Generates a new 6-digit OTP and invalidates any previous unused OTPs. A cooldown of 60 seconds applies between requests. Returns the new OTP (non-production only; in production this will be sent via email). Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "New OTP generated and returned"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already verified", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "429", description = "Cooldown active, too many requests", content = @Content)
        }
    )
    public ResponseEntity<ResendVerificationResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        String otp = emailVerificationService.resendOtp(request);
        return ResponseEntity.ok(new ResendVerificationResponse(otp));
    }
}
