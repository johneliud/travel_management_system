package com.travelmanagementsystem.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import java.util.List;

import org.junit.jupiter.api.DisplayName;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.travelmanagementsystem")
@DisplayName("Architecture Tests - Module Boundaries")
class ArchitectureTest {

	private static final String BASE_PACKAGE = "com.travelmanagementsystem";

	private static final List<String> MODULES = List.of(
		"identity", "travel", "subscription", "payment", "feedback",
		"reporting", "search", "recommendation", "ai", "analytics", "notification");

	private static String[] modulePackages(String suffix) {
		return MODULES.stream()
			.map(m -> BASE_PACKAGE + "." + m + suffix)
			.toArray(String[]::new);
	}

	private static String[] modulePackages(String... suffixes) {
		return MODULES.stream()
			.flatMap(m -> java.util.Arrays.stream(suffixes)
				.map(suffix -> BASE_PACKAGE + "." + m + suffix))
			.distinct()
			.toArray(String[]::new);
	}

	@ArchTest
	static final ArchRule domain_layer_must_not_depend_on_infrastructure =
		classes()
			.that().resideInAPackage(BASE_PACKAGE + "..domain..")
			.and().resideOutsideOfPackage(BASE_PACKAGE + ".shared..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + "..domain..",
				BASE_PACKAGE + "..api..",
				BASE_PACKAGE + "..shared..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..",
				"org.hibernate..",
				"tools.jackson..",
				"org.slf4j..")
			.as("Domain layer must not depend on infrastructure layer");

	@ArchTest
	static final ArchRule application_layer_must_not_depend_on_infrastructure =
		classes()
			.that().resideInAPackage(BASE_PACKAGE + "..application..")
			.and().resideOutsideOfPackage(BASE_PACKAGE + ".shared..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + "..api..",
				BASE_PACKAGE + "..application..",
				BASE_PACKAGE + "..domain..",
				BASE_PACKAGE + "..shared..",
				BASE_PACKAGE + "..infrastructure..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..",
				"io.jsonwebtoken..",
				"org.junit..",
				"org.assertj..",
				"org.slf4j..")
			.as("Application layer may depend on infrastructure repositories");

	@ArchTest
	static final ArchRule module_api_layers_must_not_depend_on_domain_or_infrastructure =
		classes()
			.that().resideInAnyPackage(modulePackages("..api.."))
			.and().resideOutsideOfPackage(BASE_PACKAGE + ".shared..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + "..api..",
				BASE_PACKAGE + "..application..",
				BASE_PACKAGE + "..shared..",
				"java..",
				"javax..",
				"jakarta..",
				"io.swagger.v3.oas.annotations..",
				"org.springframework..",
				"org.junit..",
				"org.assertj..",
				"org.springframework.test.web.servlet..")
			.as("Module API layer depends on application layer and OpenAPI annotations");

	@ArchTest
	static final ArchRule shared_must_not_depend_on_any_module =
		classes()
			.that().resideInAPackage(BASE_PACKAGE + ".shared..")
			.and().resideOutsideOfPackage(BASE_PACKAGE + ".shared.api..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + ".shared..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..",
				"io.swagger.v3.oas.models..",
				"org.testcontainers..",
				"org.junit..",
				"org.slf4j..")
			.as("Shared package must not depend on any module");

	@ArchTest
	static final ArchRule shared_api_may_depend_on_shared_exceptions =
		classes()
			.that().resideInAPackage(BASE_PACKAGE + ".shared.api..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + ".shared..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..",
				"org.slf4j..",
				"com.fasterxml..",
				"tools.jackson..",
				"org.junit..",
				"org.mockito..",
				"org.assertj..")
			.as("Shared API may depend on shared exceptions and Jackson");
}
