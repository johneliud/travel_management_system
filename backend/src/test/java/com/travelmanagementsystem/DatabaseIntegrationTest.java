package com.travelmanagementsystem;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Database Integration Tests")
class DatabaseIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
		.withDatabaseName("travelmanagementsystem_test")
		.withUsername("test")
		.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}

	@Autowired
	private DataSource dataSource;

	@Test
	@DisplayName("Should connect to PostgreSQL via Testcontainers")
	void shouldConnectToPostgreSQL() throws Exception {
		assertThat(dataSource).isNotNull();
		assertThat(dataSource.getConnection()).isNotNull();
		assertThat(dataSource.getConnection().isValid(5)).isTrue();
	}

	@Test
	@DisplayName("Should have flyway_schema_history table after migration")
	void shouldHaveFlywaySchemaHistoryTable() throws Exception {
		var connection = dataSource.getConnection();
		var resultSet = connection.getMetaData().getTables(
			null, null, "flyway_schema_history", new String[]{"TABLE"});
		assertThat(resultSet.next()).isTrue();
		resultSet.close();
		connection.close();
	}

	@Test
	@DisplayName("Should have baseline migration recorded")
	void shouldHaveBaselineMigrationRecorded() throws Exception {
		var connection = dataSource.getConnection();
		var statement = connection.prepareStatement(
			"SELECT success FROM flyway_schema_history WHERE version = '1'");
		var resultSet = statement.executeQuery();
		assertThat(resultSet.next()).isTrue();
		assertThat(resultSet.getBoolean("success")).isTrue();
		resultSet.close();
		statement.close();
		connection.close();
	}
}
