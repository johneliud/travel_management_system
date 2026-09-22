package com.travelmanagementsystem.identity.application;

import com.travelmanagementsystem.identity.api.ChangePasswordRequest;
import com.travelmanagementsystem.identity.api.ProfileResponse;
import com.travelmanagementsystem.identity.api.UpdateProfileRequest;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.domain.VerificationTokenType;
import com.travelmanagementsystem.identity.infrastructure.persistence.RefreshTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.exception.ConflictException;
import com.travelmanagementsystem.shared.exception.InvalidPasswordException;
import com.travelmanagementsystem.shared.exception.NotFoundException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationService emailVerificationService;

    public UserProfileService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of(User.class, userId));
        return toProfile(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of(User.class, userId));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                log.debug("Profile update attempt with duplicate email for user with id {}", userId);
                throw ConflictException.of("EMAIL_ALREADY_IN_USE", "email already in use");
            }
            user.setEmail(request.email());
            user.setEmailVerified(false);

            log.debug("User with id {} changed email, email verification reset", userId);
        }

        return toProfile(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of(User.class, userId));

        emailVerificationService.verifyOtp(user, request.otp(), VerificationTokenType.PASSWORD_CHANGE);

        if (!passwordHasher.verify(request.currentPassword(), user.getPasswordHash())) {
            log.debug("Password change attempt with wrong current password for user with id {}", userId);
            throw InvalidPasswordException.of();
        }

        user.setPasswordHash(passwordHasher.hash(request.newPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        log.debug("Password changed for user with id {}, all refresh tokens revoked", userId);
    }

    private ProfileResponse toProfile(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()),
                user.getCreatedAt());
    }
}
