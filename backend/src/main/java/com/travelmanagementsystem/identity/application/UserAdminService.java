package com.travelmanagementsystem.identity.application;

import com.travelmanagementsystem.identity.api.UserDetailResponse;
import com.travelmanagementsystem.identity.api.UserSummaryResponse;
import com.travelmanagementsystem.identity.domain.Role;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.infrastructure.persistence.RefreshTokenRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.RoleRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.exception.NotFoundException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(String status, String email, String role, Pageable pageable) {
        String emailPattern = (email != null && !email.isBlank()) ? "%" + email + "%" : null;
        return userRepository.findByFilters(status, emailPattern, role, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of(User.class, id));
        return toDetail(user);
    }

    @Transactional
    public void updateUserStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of(User.class, id));

        String oldStatus = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);

        if ("SUSPENDED".equals(status)) {
            refreshTokenRepository.deleteByUser(user);
            log.debug("Revoked all refresh tokens for suspended user with id {}", id);
        }

        log.debug("User with id {} status changed from {} to {}", id, oldStatus, status);
    }

    @Transactional
    public void updateUserRole(Long id, String roleName) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> NotFoundException.of(User.class, id));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        Set<Role> currentRoles = user.getRoles();
        Set<String> currentRoleNames = currentRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        if (currentRoleNames.contains(roleName)) {
            return;
        }

        currentRoles.clear();
        currentRoles.add(role);
        userRepository.save(user);

        log.debug("User with id {} role changed to {}", id, roleName);
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                user.getCreatedAt());
    }

    private UserDetailResponse toDetail(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
