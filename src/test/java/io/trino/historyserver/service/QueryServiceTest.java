package io.trino.historyserver.service;

import io.trino.historyserver.dto.QueryReference;
import io.trino.historyserver.exception.QueryFetchException;
import io.trino.historyserver.exception.QueryStorageException;
import io.trino.historyserver.fetch.TrinoQueryFetcher;
import io.trino.historyserver.storage.RetryingStorageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private TrinoQueryFetcher trinoQueryFetcher;

    @Mock
    private RetryingStorageHandler storageHandler;

    private QueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new QueryService(trinoQueryFetcher, storageHandler);
    }

    @Test
    void createQuery_ShouldSuccessfullyCreateQuery_WhenValidInput() {
        // Given
        QueryReference queryRef = new QueryReference("test-query-id", "http://localhost:8080");
        String environment = "test";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(trinoQueryFetcher.fetchQuery(queryRef)).thenReturn(expectedQueryJson);
        doNothing().when(storageHandler).writeQuery(queryRef.queryId(), environment, expectedQueryJson);

        // When
        assertDoesNotThrow(() -> queryService.createQuery(queryRef, environment));

        // Then
        verify(trinoQueryFetcher).fetchQuery(queryRef);
        verify(storageHandler).writeQuery(queryRef.queryId(), environment, expectedQueryJson);
    }

    @Test
    void createQuery_ShouldThrowException_WhenFetchFails() {
        // Given
        QueryReference queryRef = new QueryReference("test-query-id", "http://localhost:8080");
        String environment = "test";

        when(trinoQueryFetcher.fetchQuery(queryRef))
                .thenThrow(new QueryFetchException("test-query-id", "Failed to fetch query"));

        // When & Then
        assertThrows(QueryFetchException.class, () -> queryService.createQuery(queryRef, environment));
        verify(trinoQueryFetcher).fetchQuery(queryRef);
        verify(storageHandler, never()).writeQuery(anyString(), anyString(), anyString());
    }

    @Test
    void createQuery_ShouldThrowException_WhenStorageFails() {
        // Given
        QueryReference queryRef = new QueryReference("test-query-id", "http://localhost:8080");
        String environment = "test";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(trinoQueryFetcher.fetchQuery(queryRef)).thenReturn(expectedQueryJson);
        doThrow(new QueryStorageException("test-query-id", "Storage failed"))
                .when(storageHandler).writeQuery(queryRef.queryId(), environment, expectedQueryJson);

        // When & Then
        assertThrows(QueryStorageException.class, () -> queryService.createQuery(queryRef, environment));
        verify(trinoQueryFetcher).fetchQuery(queryRef);
        verify(storageHandler).writeQuery(queryRef.queryId(), environment, expectedQueryJson);
    }

    @Test
    void getQuery_ShouldReturnQueryJson_WhenQueryExists() {
        // Given
        String queryId = "test-query-id";
        String environment = "test";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(storageHandler.readQuery(queryId, environment)).thenReturn(expectedQueryJson);

        // When
        String result = queryService.getQuery(queryId, environment);

        // Then
        assertEquals(expectedQueryJson, result);
        verify(storageHandler).readQuery(queryId, environment);
    }

    @Test
    void getQuery_ShouldThrowException_WhenStorageFails() {
        // Given
        String queryId = "test-query-id";
        String environment = "test";

        when(storageHandler.readQuery(queryId, environment))
                .thenThrow(new QueryStorageException(queryId, "Query not found"));

        // When & Then
        assertThrows(QueryStorageException.class, () -> queryService.getQuery(queryId, environment));
        verify(storageHandler).readQuery(queryId, environment);
    }

    @Test
    void getQuery_ShouldHandleNullEnvironment() {
        // Given
        String queryId = "test-query-id";
        String environment = null;
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(storageHandler.readQuery(queryId, environment)).thenReturn(expectedQueryJson);

        // When
        String result = queryService.getQuery(queryId, environment);

        // Then
        assertEquals(expectedQueryJson, result);
        verify(storageHandler).readQuery(queryId, environment);
    }
}
