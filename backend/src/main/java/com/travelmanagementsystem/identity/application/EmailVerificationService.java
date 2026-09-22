package com.travelmanagementsystem.identity.application;

import com.travelmanagementsystem.identity.api.ResendVerificationRequest;
import com.travelmanagementsystem.identity.api.VerificationRequest;
import com.travelmanagementsystem.identity.domain.EmailVerificationToken;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.domain.VerificationTokenType;
import com.travelmanagementsystem.identity.infrastructure.persistence.EmailVerificationTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.exception.BusinessException;
import com.travelmanagementsystem.shared.exception.InvalidVerificationTokenException;
import com.travelmanagementsystem.shared.exception.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int OTP_LENGTH = 6;
    private static final SecureRandom secureRandom = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${email-verification.validity-hours}")
    private long validityHours;

    @Value("${email-verification.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String generateOtp(User user, VerificationTokenType type) {
        invalidateUnusedTokens(user, type);

        String otp = generateNumericOtp();
        String tokenHash = hashOtp(otp);
        Instant expiresAt = Instant.now().plus(validityHours, ChronoUnit.HOURS);

        EmailVerificationToken token = new EmailVerificationToken(user, tokenHash, expiresAt, type);
        tokenRepository.save(token);

        log.debug("Generated {} OTP for user with id {}", type, user.getId());

        return otp;
    }

    @Transactional
    public String generatePasswordChangeOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of(User.class, userId));
        return generateOtp(user, VerificationTokenType.PASSWORD_CHANGE);
    }

    private void invalidateUnusedTokens(User user, VerificationTokenType type) {
        List<EmailVerificationToken> tokens = tokenRepository.findAllByUserAndTypeOrderByCreatedAtDesc(user, type);
        for (EmailVerificationToken token : tokens) {
            if (!token.isUsed()) {
                token.markUsed();
                tokenRepository.save(token);
            }
        }
    }

    @Transactional
    public String resendOtp(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> NotFoundException.of(User.class, request.email()));

        if (user.getEmailVerified()) {
            throw BusinessException.of("EMAIL_ALREADY_VERIFIED", "email is already verified");
        }

        List<EmailVerificationToken> tokens = tokenRepository.findAllByUserAndTypeOrderByCreatedAtDesc(
                user, VerificationTokenType.EMAIL_VERIFICATION);

        Optional<EmailVerificationToken> mostRecentUnused = tokens.stream()
                .filter(t -> !t.isUsed())
                .findFirst();

        if (mostRecentUnused.isPresent()) {
            Instant lastCreatedAt = mostRecentUnused.get().getCreatedAt();
            long secondsSince = Duration.between(lastCreatedAt, Instant.now()).getSeconds();
            if (secondsSince < resendCooldownSeconds) {
                long waitSeconds = resendCooldownSeconds - secondsSince;
                throw BusinessException.of("RESEND_COOLDOWN",
                        "please wait %d seconds before requesting a new code".formatted(waitSeconds));
            }
        }

        String otp = generateOtp(user, VerificationTokenType.EMAIL_VERIFICATION);

        log.debug("Resent verification OTP for user with id {}", user.getId());

        return otp;
    }

    @Transactional
    public void verify(VerificationRequest request) {
        String tokenHash = hashOtp(request.otp());

        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidVerificationTokenException::notFound);

        if (token.getType() != VerificationTokenType.EMAIL_VERIFICATION) {
            throw InvalidVerificationTokenException.notFound();
        }

        if (token.isExpired()) {
            log.debug("Verification token {} expired", token.getId());
            throw InvalidVerificationTokenException.expired();
        }

        if (token.isUsed()) {
            log.debug("Verification token {} already used", token.getId());
            throw InvalidVerificationTokenException.alreadyUsed();
        }

        token.markUsed();
        tokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        log.debug("Email verified for user with id {}", user.getId());
    }

    @Transactional
    public void verifyOtp(User user, String otp, VerificationTokenType expectedType) {
        String tokenHash = hashOtp(otp);

        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidVerificationTokenException::notFound);

        if (token.getType() != expectedType) {
            throw InvalidVerificationTokenException.notFound();
        }

        if (!token.getUser().getId().equals(user.getId())) {
            throw InvalidVerificationTokenException.notFound();
        }

        if (token.isExpired()) {
            throw InvalidVerificationTokenException.expired();
        }

        if (token.isUsed()) {
            throw InvalidVerificationTokenException.alreadyUsed();
        }

        token.markUsed();
        tokenRepository.save(token);

        log.debug("OTP verified (type={}) for user with id {}", expectedType, user.getId());
    }

    private String generateNumericOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
