package io.trino.historyserver.storage;

import io.trino.historyserver.exception.QueryStorageException;
import io.trino.historyserver.util.TaskRetryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryingStorageHandlerTest {

    @Mock
    private RetryingStorageHandlerProperties properties;

    @Mock
    private QueryStorageHandler delegate;

    @Mock
    private TaskRetryExecutor taskRetryExecutor;

    private RetryingStorageHandler retryingStorageHandler;

    @BeforeEach
    void setUp() {
        retryingStorageHandler = new RetryingStorageHandler(properties, delegate, taskRetryExecutor);
        
        when(properties.getMaxRetries()).thenReturn(3);
        when(properties.getBackoffMillis()).thenReturn(100L);
    }

    @Test
    void writeQuery_ShouldDelegateToTaskRetryExecutor() throws Exception {
        // Given
        String queryId = "test-query-id";
        String environment = "test";
        String queryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        doNothing().when(taskRetryExecutor).executeWithRetry(any(Runnable.class), eq(3), eq(100L));

        // When
        retryingStorageHandler.writeQuery(queryId, environment, queryJson);

        // Then
        verify(taskRetryExecutor).executeWithRetry(any(Runnable.class), eq(3), eq(100L));
    }

    @Test
    void writeQuery_ShouldPropagateException_WhenRetryFails() throws Exception {
        // Given
        String queryId = "test-query-id";
        String environment = "test";
        String queryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        QueryStorageException expectedException = new QueryStorageException(queryId, "Storage failed");
        doThrow(expectedException).when(taskRetryExecutor).executeWithRetry(any(Runnable.class), eq(3), eq(100L));

        // When & Then
        assertThrows(QueryStorageException.class, () -> 
            retryingStorageHandler.writeQuery(queryId, environment, queryJson));
        
        verify(taskRetryExecutor).executeWithRetry(any(Runnable.class), eq(3), eq(100L));
    }

    @Test
    void readQuery_ShouldDelegateToTaskRetryExecutor() throws Exception {
        // Given
        String queryId = "test-query-id";
        String environment = "test";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(taskRetryExecutor.executeWithRetry(any(Supplier.class), eq(3), eq(100L))).thenReturn(expectedQueryJson);

        // When
        String result = retryingStorageHandler.readQuery(queryId, environment);

        // Then
        assertEquals(expectedQueryJson, result);
        verify(taskRetryExecutor).executeWithRetry(any(Supplier.class), eq(3), eq(100L));
    }

    @Test
    void readQuery_ShouldPropagateException_WhenRetryFails() throws Exception {
        // Given
        String queryId = "test-query-id";
        String environment = "test";

        QueryStorageException expectedException = new QueryStorageException(queryId, "Query not found");
        when(taskRetryExecutor.executeWithRetry(any(Supplier.class), eq(3), eq(100L))).thenThrow(expectedException);

        // When & Then
        assertThrows(QueryStorageException.class, () -> 
            retryingStorageHandler.readQuery(queryId, environment));
        
        verify(taskRetryExecutor).executeWithRetry(any(Supplier.class), eq(3), eq(100L));
    }

    @Test
    void writeQuery_ShouldUseCorrectRetryConfiguration() throws Exception {
        // Given
        String queryId = "test-query-id";
        String environment = "test";
        String queryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getBackoffMillis()).thenReturn(200L);

        doNothing().when(taskRetryExecutor).executeWithRetry(any(Runnable.class), eq(5), eq(200L));

        // When
        retryingStorageHandler.writeQuery(queryId, environment, queryJson);

        // Then
        verify(taskRetryExecutor).executeWithRetry(any(Runnable.class), eq(5), eq(200L));
    }

    @Test
    void readQuery_ShouldUseCorrectRetryConfiguration() throws Exception {
        // Given
        String queryId = "test-query-id";
        String environment = "test";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getBackoffMillis()).thenReturn(200L);

        when(taskRetryExecutor.executeWithRetry(any(Supplier.class), eq(5), eq(200L))).thenReturn(expectedQueryJson);

        // When
        String result = retryingStorageHandler.readQuery(queryId, environment);

        // Then
        assertEquals(expectedQueryJson, result);
        verify(taskRetryExecutor).executeWithRetry(any(Supplier.class), eq(5), eq(200L));
    }
}
