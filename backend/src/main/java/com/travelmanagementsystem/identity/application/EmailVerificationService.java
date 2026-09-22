package com.travelmanagementsystem.identity.application;

import com.travelmanagementsystem.identity.api.VerificationRequest;
import com.travelmanagementsystem.identity.domain.EmailVerificationToken;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.EmailVerificationTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.exception.InvalidVerificationTokenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
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

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String generateOtp(User user) {
        String otp = generateNumericOtp();
        String tokenHash = hashOtp(otp);
        Instant expiresAt = Instant.now().plus(validityHours, ChronoUnit.HOURS);

        EmailVerificationToken token = new EmailVerificationToken(user, tokenHash, expiresAt);
        tokenRepository.save(token);

        log.debug("Generated verification OTP {} for user with id {}", otp, user.getId());

        return otp;
    }

    @Transactional
    public void verify(VerificationRequest request) {
        String tokenHash = hashOtp(request.otp());

        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidVerificationTokenException::notFound);

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
