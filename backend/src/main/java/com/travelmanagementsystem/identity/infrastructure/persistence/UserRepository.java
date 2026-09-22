package com.travelmanagementsystem.identity.infrastructure.persistence;

import com.travelmanagementsystem.identity.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:email IS NULL OR u.email LIKE :email) AND " +
           "(:role IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE r.name = :role))")
    Page<User> findByFilters(
            @Param("status") String status,
            @Param("email") String email,
            @Param("role") String role,
            Pageable pageable);
}
