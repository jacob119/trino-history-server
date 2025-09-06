package io.trino.historyserver.controller;

import io.trino.historyserver.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(globalExceptionHandler).build();
    }

    @Test
    void handleInvalidQueryEventException_ShouldReturnBadRequest() {
        // Given
        InvalidQueryEventException exception = new InvalidQueryEventException("Invalid query event");

        // When
        ResponseEntity<String> response = globalExceptionHandler.handleInvalidEventError(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Received invalid query event: Invalid query event"));
    }

    @Test
    void handleTrinoAuthException_ShouldReturnInternalServerError() {
        // Given
        TrinoAuthException exception = new TrinoAuthException("Authentication failed");

        // When
        ResponseEntity<String> response = globalExceptionHandler.handleTrinoAuthError(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to authenticate with the coordinator: Authentication failed"));
    }

    @Test
    void handleQueryFetchException_ShouldReturnInternalServerError() {
        // Given
        QueryFetchException exception = new QueryFetchException("Failed to fetch query", "test-query-id");

        // When
        ResponseEntity<String> response = globalExceptionHandler.handleFetchError(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to fetch query from coordinator: Failed to fetch query"));
    }

    @Test
    void handleQueryStorageException_ShouldReturnInternalServerError() {
        // Given
        QueryStorageException exception = new QueryStorageException("Storage failed", "test-query-id");

        // When
        ResponseEntity<String> response = globalExceptionHandler.handleStorageError(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error handling query file: Storage failed"));
    }

    @Test
    void handleStorageInitializationException_ShouldReturnInternalServerError() {
        // Given
        StorageInitializationException exception = new StorageInitializationException("Storage init failed");

        // When
        ResponseEntity<String> response = globalExceptionHandler.handleStorageInitError(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error initializing storage: Storage init failed"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<String> response = globalExceptionHandler.handleGenericError(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Something went wrong: Unexpected error"));
    }
}
