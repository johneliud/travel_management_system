package com.travelmanagementsystem.identity.infrastructure.persistence;

import com.travelmanagementsystem.identity.domain.Role;
import com.travelmanagementsystem.shared.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IdentityMigrationTest extends IntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void migrationAppliesSuccessfully() {
        List<Role> roles = roleRepository.findAll();
        assert roles.size() == 3;
    }

    @Test
    void seededRolesAreQueryable() {
        assert roleRepository.findByName("ADMIN").isPresent();
        assert roleRepository.findByName("TRAVEL_MANAGER").isPresent();
        assert roleRepository.findByName("TRAVELER").isPresent();
    }
}
