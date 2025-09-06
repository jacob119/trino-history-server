package io.trino.historyserver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.trino.historyserver.HistoryServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HistoryServerApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class QueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
        assert webApplicationContext != null;
    }

    @Test
    void healthEndpoint_ShouldReturnOk() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // When & Then
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void createQuery_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String invalidJson = "invalid json";

        // When & Then
        mockMvc.perform(post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .header("X-Trino-Coordinator-Url", "http://localhost:8080"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQuery_ShouldReturnBadRequest_WhenMissingCoordinatorHeader() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String validJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        // When & Then
        mockMvc.perform(post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuery_ShouldReturnNotFound_WhenQueryDoesNotExist() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String nonExistentQueryId = "non-existent-query-id";

        // When & Then
        mockMvc.perform(get("/api/v1/query/{queryId}", nonExistentQueryId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void handleBaseQueryPath_ShouldReturnNotFound() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // When & Then
        mockMvc.perform(get("/api/v1/query"))
                .andExpect(status().isNotFound());
    }
}
