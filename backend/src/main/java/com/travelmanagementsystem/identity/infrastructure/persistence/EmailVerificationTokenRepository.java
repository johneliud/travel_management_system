package com.travelmanagementsystem.identity.infrastructure.persistence;

import com.travelmanagementsystem.identity.domain.EmailVerificationToken;
import com.travelmanagementsystem.identity.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByUserAndUsedFalse(User user);
}
