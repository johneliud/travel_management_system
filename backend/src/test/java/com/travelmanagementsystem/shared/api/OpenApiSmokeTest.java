package com.travelmanagementsystem.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OpenAPI Smoke Tests")
class OpenApiSmokeTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Should return OpenAPI spec at /v3/api-docs")
	void shouldReturnOpenApiSpec() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info").exists());
	}

	@Test
	@DisplayName("Should return Swagger UI at /swagger-ui.html")
	void shouldReturnSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection());
	}
}
