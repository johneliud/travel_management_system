package com.travelmanagementsystem.identity.infrastructure.persistence;

import com.travelmanagementsystem.identity.domain.RefreshToken;
import com.travelmanagementsystem.identity.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findByUser(User user);

    void deleteByUser(User user);
}
