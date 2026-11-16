package com.nikitarybalko.wargamebroker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.support.AbstractPostgresIntegrationTest;
import com.nikitarybalko.wargamebroker.support.TestEndpoints;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.nikitarybalko.wargamebroker.support.TestCases.TC_NFR7_01;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag(TC_NFR7_01)
@DisplayName("Integration tests: OpenAPI documentation")
class OpenApiDocumentationIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PATHS = "paths";
    private static final String SECURITY_SCHEME = "/components/securitySchemes/ApiKeyAuth";
    private static final String SCHEME_HEADER_NAME = "name";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI specification is public and documents every REST endpoint")
    void specificationDocumentsAllEndpoints() throws Exception {
        String body = mockMvc.perform(get(TestEndpoints.API_DOCS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode specification = objectMapper.readTree(body);

        List<String> missing = TestEndpoints.ALL_API_PATHS.stream()
                .filter(path -> !specification.path(PATHS).has(path))
                .toList();
        assertTrue(missing.isEmpty(), "Endpoints missing from the OpenAPI specification: " + missing);
    }

    @Test
    @DisplayName("OpenAPI specification declares the X-API-KEY header security scheme")
    void specificationDeclaresApiKeyScheme() throws Exception {
        String body = mockMvc.perform(get(TestEndpoints.API_DOCS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode scheme = objectMapper.readTree(body).at(SECURITY_SCHEME);
        assertEquals(ApiKeyAuthFilter.API_KEY_HEADER, scheme.path(SCHEME_HEADER_NAME).asText());
    }

    @Test
    @DisplayName("Swagger UI is served without an API key")
    void swaggerUiIsAvailable() throws Exception {
        mockMvc.perform(get(TestEndpoints.SWAGGER_UI))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get(TestEndpoints.SWAGGER_UI_INDEX))
                .andExpect(status().isOk());
    }
}
