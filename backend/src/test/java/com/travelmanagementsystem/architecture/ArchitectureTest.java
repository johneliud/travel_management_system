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
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + "..api..",
				BASE_PACKAGE + "..application..",
				BASE_PACKAGE + "..domain..",
				BASE_PACKAGE + "..shared..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..",
				"org.slf4j..")
			.as("Application layer must not depend on infrastructure layer");

	@ArchTest
	static final ArchRule api_layer_must_not_depend_on_domain_or_infrastructure =
		classes()
			.that().resideInAPackage(BASE_PACKAGE + "..api..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + "..api..",
				BASE_PACKAGE + "..shared..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..")
			.as("API layer must not depend on domain or infrastructure layers");

	@ArchTest
	static final ArchRule shared_must_not_depend_on_any_module =
		classes()
			.that().resideInAPackage(BASE_PACKAGE + ".shared..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(
				BASE_PACKAGE + ".shared..",
				"java..",
				"javax..",
				"jakarta..",
				"org.springframework..",
				"org.slf4j..")
			.as("Shared package must not depend on any module");
}
