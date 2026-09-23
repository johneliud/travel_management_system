package com.travelmanagementsystem.identity.application;

import com.travelmanagementsystem.identity.api.RegisterRequest;
import com.travelmanagementsystem.identity.api.RegisterResponse;
import com.travelmanagementsystem.identity.domain.Role;
import com.travelmanagementsystem.identity.domain.User;
import com.travelmanagementsystem.identity.domain.VerificationTokenType;
import com.travelmanagementsystem.identity.infrastructure.persistence.RoleRepository;
import com.travelmanagementsystem.identity.infrastructure.persistence.UserRepository;
import com.travelmanagementsystem.shared.exception.ConflictException;
import com.travelmanagementsystem.shared.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationService emailVerificationService;

    public RegistrationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.debug("Registration attempt with duplicate email");
            throw ConflictException.of("EMAIL_ALREADY_IN_USE", "email already in use");
        }

        String hashedPassword = passwordHasher.hash(request.password());

        User user = new User(request.email(), hashedPassword, request.firstName(), request.lastName());

        Role travelerRole = roleRepository.findByName(Roles.TRAVELER)
            .orElseThrow(() -> new IllegalStateException("TRAVELER role not found"));
        user.addRole(travelerRole);

        User saved = userRepository.save(user);

        String otp = emailVerificationService.generateOtp(saved, VerificationTokenType.EMAIL_VERIFICATION);

        log.debug("Registered new user with id {}", saved.getId());

        return new RegisterResponse(
            saved.getId(),
            saved.getFirstName(),
            saved.getLastName(),
            saved.getEmail(),
            saved.getStatus(),
            saved.getCreatedAt(),
            otp
        );
    }
}
