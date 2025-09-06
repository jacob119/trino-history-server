package io.trino.historyserver.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void invalidQueryEventException_ShouldCreateWithMessage() {
        // Given
        String message = "Invalid query event";

        // When
        InvalidQueryEventException exception = new InvalidQueryEventException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void invalidQueryEventException_ShouldCreateWithMessageAndCause() {
        // Given
        String message = "Invalid query event";
        Throwable cause = new RuntimeException("Root cause");

        // When
        InvalidQueryEventException exception = new InvalidQueryEventException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void trinoAuthException_ShouldCreateWithMessage() {
        // Given
        String message = "Authentication failed";

        // When
        TrinoAuthException exception = new TrinoAuthException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void trinoAuthException_ShouldCreateWithMessageAndCause() {
        // Given
        String message = "Authentication failed";
        Throwable cause = new RuntimeException("Root cause");

        // When
        TrinoAuthException exception = new TrinoAuthException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void queryFetchException_ShouldCreateWithMessageAndQueryId() {
        // Given
        String message = "Failed to fetch query";
        String queryId = "test-query-id";

        // When
        QueryFetchException exception = new QueryFetchException(message, queryId);

        // Then
        assertEquals(queryId, exception.getQueryId());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void queryFetchException_ShouldCreateWithMessageQueryIdAndCause() {
        // Given
        String message = "Failed to fetch query";
        String queryId = "test-query-id";
        Throwable cause = new RuntimeException("Root cause");

        // When
        QueryFetchException exception = new QueryFetchException(message, queryId, cause);

        // Then
        assertEquals(queryId, exception.getQueryId());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void queryStorageException_ShouldCreateWithMessageAndQueryId() {
        // Given
        String message = "Storage failed";
        String queryId = "test-query-id";

        // When
        QueryStorageException exception = new QueryStorageException(message, queryId);

        // Then
        assertEquals(queryId, exception.getQueryId());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void queryStorageException_ShouldCreateWithMessageQueryIdAndCause() {
        // Given
        String message = "Storage failed";
        String queryId = "test-query-id";
        Throwable cause = new RuntimeException("Root cause");

        // When
        QueryStorageException exception = new QueryStorageException(message, queryId, cause);

        // Then
        assertEquals(queryId, exception.getQueryId());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void storageInitializationException_ShouldCreateWithMessage() {
        // Given
        String message = "Storage initialization failed";

        // When
        StorageInitializationException exception = new StorageInitializationException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void storageInitializationException_ShouldCreateWithMessageAndCause() {
        // Given
        String message = "Storage initialization failed";
        Throwable cause = new RuntimeException("Root cause");

        // When
        StorageInitializationException exception = new StorageInitializationException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void expiredSessionException_ShouldCreateWithMessage() {
        // Given
        String message = "Session expired";

        // When
        ExpiredSessionException exception = new ExpiredSessionException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void expiredSessionException_ShouldCreateWithMessageAndCause() {
        // Given
        String message = "Session expired";
        Throwable cause = new RuntimeException("Root cause");

        // When
        ExpiredSessionException exception = new ExpiredSessionException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void queryException_ShouldCreateWithMessageAndQueryId() {
        // Given
        String message = "Query error";
        String queryId = "test-query-id";

        // When
        QueryException exception = new QueryException(message, queryId);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(queryId, exception.getQueryId());
        assertNull(exception.getCause());
    }

    @Test
    void queryException_ShouldCreateWithMessageQueryIdAndCause() {
        // Given
        String message = "Query error";
        String queryId = "test-query-id";
        Throwable cause = new RuntimeException("Root cause");

        // When
        QueryException exception = new QueryException(message, queryId, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(queryId, exception.getQueryId());
        assertEquals(cause, exception.getCause());
    }
}
