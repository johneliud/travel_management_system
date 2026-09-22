package com.travelmanagementsystem.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/*
 * Shared base class for integration tests that require a real PostgreSQL database.
 *
 * The container is started once in a static initializer block instead of using
 * @Testcontainers/@Container. Annotation-managed lifecycle stops the container
 * after each subclass finishes, but @DynamicPropertySource only fires once per
 * cached ApplicationContext — causing subsequent subclasses to hit a dead port.
 */
@SpringBootTest
@ActiveProfiles("integration")
public abstract class IntegrationTest {

	static final PostgreSQLContainer<?> postgres;

	static {
		postgres = new PostgreSQLContainer<>("postgres:18")
			.withDatabaseName("travelmanagementsystem_test")
			.withUsername("test")
			.withPassword("test");
		postgres.start();
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}
}
