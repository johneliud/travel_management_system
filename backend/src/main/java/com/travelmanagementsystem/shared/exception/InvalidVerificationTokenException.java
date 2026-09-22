package com.travelmanagementsystem.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends AuthenticationException {

    private InvalidVerificationTokenException(String message) {
        super("INVALID_VERIFICATION_OTP", message, HttpStatus.UNAUTHORIZED);
    }

    public static InvalidVerificationTokenException expired() {
        return new InvalidVerificationTokenException("Verification OTP has expired");
    }

    public static InvalidVerificationTokenException alreadyUsed() {
        return new InvalidVerificationTokenException("Verification OTP has already been used");
    }

    public static InvalidVerificationTokenException notFound() {
        return new InvalidVerificationTokenException("Invalid verification OTP");
    }
}
