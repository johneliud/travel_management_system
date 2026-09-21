package com.travelmanagementsystem.identity.application;

import com.travelmanagementsystem.identity.api.LoginRequest;
import com.travelmanagementsystem.identity.api.LoginResponse;
import com.travelmanagementsystem.identity.api.LogoutRequest;
import com.travelmanagementsystem.identity.api.RefreshRequest;
import com.travelmanagementsystem.identity.domain.RefreshToken;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.RefreshTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.exception.AccountDisabledException;
import com.travelmanagementsystem.shared.exception.InvalidCredentialsException;
import com.travelmanagementsystem.shared.exception.InvalidRefreshTokenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::of);

        if (!passwordHasher.verify(request.password(), user.getPasswordHash())) {
            log.debug("Invalid credentials for user with id {}", user.getId());
            throw InvalidCredentialsException.of();
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            log.debug("Account inactive for user with id {}", user.getId());
            throw AccountDisabledException.of(user.getStatus());
        }

        return issueTokenPair(user);
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        String tokenHash = hashRefreshToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> InvalidRefreshTokenException.notFound());

        if (refreshToken.isExpired()) {
            log.debug("Refresh token {} expired", refreshToken.getId());
            throw InvalidRefreshTokenException.expired();
        }

        if (refreshToken.isRevoked()) {
            log.debug("Refresh token {} revoked", refreshToken.getId());
            throw InvalidRefreshTokenException.revoked();
        }

        User user = refreshToken.getUser();

        if (!"ACTIVE".equals(user.getStatus())) {
            log.debug("Account inactive for user with id {}", user.getId());
            throw AccountDisabledException.of(user.getStatus());
        }

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token {} revoked, issuing new pair for user with id {}", refreshToken.getId(), user.getId());

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = hashRefreshToken(request.refreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.revoke();
                        refreshTokenRepository.save(token);

                        log.debug("Refresh token {} revoked via logout", token.getId());
                    }
                });
    }

    private LoginResponse issueTokenPair(User user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()));

        String accessToken = jwtService.generateAccessToken(claims);
        String refreshToken = jwtService.generateRefreshToken(claims);

        String refreshTokenHash = hashRefreshToken(refreshToken);
        Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs());
        RefreshToken persistedToken = new RefreshToken(user, refreshTokenHash, expiresAt);
        refreshTokenRepository.save(persistedToken);

        log.debug("Issued new token pair for user with id {}", user.getId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000);
    }

    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
