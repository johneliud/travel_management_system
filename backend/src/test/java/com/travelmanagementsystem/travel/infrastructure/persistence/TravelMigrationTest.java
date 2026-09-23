package com.travelmanagementsystem.travel.infrastructure.persistence;

import com.travelmanagementsystem.shared.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Travel Migration Test")
class TravelMigrationTest extends IntegrationTest {

    @Autowired
    private TravelRepository travelRepository;

    @BeforeEach
    void cleanUp() {
        travelRepository.deleteAll();
    }

    @Test
    void migrationAppliesSuccessfully() {
        var travels = travelRepository.findAll();
        assert travels.isEmpty();
    }
}
