package io.trino.historyserver.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryReferenceTest {

    @Test
    void constructor_ShouldCreateQueryReference_WhenValidParameters() {
        // Given
        String queryId = "test-query-id";
        String coordinatorUrl = "http://localhost:8080";

        // When
        QueryReference queryRef = new QueryReference(queryId, coordinatorUrl);

        // Then
        assertEquals(queryId, queryRef.queryId());
        assertEquals(coordinatorUrl, queryRef.coordinatorUrl());
    }

    @Test
    void constructor_ShouldThrowException_WhenQueryIdIsNull() {
        // Given
        String queryId = null;
        String coordinatorUrl = "http://localhost:8080";

        // When & Then
        assertThrows(NullPointerException.class, () -> new QueryReference(queryId, coordinatorUrl));
    }

    @Test
    void constructor_ShouldThrowException_WhenCoordinatorUrlIsNull() {
        // Given
        String queryId = "test-query-id";
        String coordinatorUrl = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> new QueryReference(queryId, coordinatorUrl));
    }

    @Test
    void equals_ShouldReturnTrue_WhenSameValues() {
        // Given
        QueryReference queryRef1 = new QueryReference("test-query-id", "http://localhost:8080");
        QueryReference queryRef2 = new QueryReference("test-query-id", "http://localhost:8080");

        // When & Then
        assertEquals(queryRef1, queryRef2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferentValues() {
        // Given
        QueryReference queryRef1 = new QueryReference("test-query-id-1", "http://localhost:8080");
        QueryReference queryRef2 = new QueryReference("test-query-id-2", "http://localhost:8080");

        // When & Then
        assertNotEquals(queryRef1, queryRef2);
    }

    @Test
    void hashCode_ShouldReturnSameValue_WhenSameValues() {
        // Given
        QueryReference queryRef1 = new QueryReference("test-query-id", "http://localhost:8080");
        QueryReference queryRef2 = new QueryReference("test-query-id", "http://localhost:8080");

        // When & Then
        assertEquals(queryRef1.hashCode(), queryRef2.hashCode());
    }

    @Test
    void toString_ShouldContainQueryIdAndCoordinatorUrl() {
        // Given
        String queryId = "test-query-id";
        String coordinatorUrl = "http://localhost:8080";
        QueryReference queryRef = new QueryReference(queryId, coordinatorUrl);

        // When
        String result = queryRef.toString();

        // Then
        assertTrue(result.contains(queryId));
        assertTrue(result.contains(coordinatorUrl));
    }
}
