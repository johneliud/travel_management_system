package com.travelmanagementsystem.identity.infrastructure.persistence;

import com.travelmanagementsystem.identity.domain.EmailVerificationToken;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.domain.VerificationTokenType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByUserAndUsedFalseAndType(User user, VerificationTokenType type);

    List<EmailVerificationToken> findAllByUserAndTypeOrderByCreatedAtDesc(User user, VerificationTokenType type);
}
