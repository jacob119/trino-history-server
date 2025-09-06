package io.trino.historyserver.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.trino.historyserver.exception.InvalidQueryEventException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryReferenceFactoryTest {

    @Mock
    private HttpServletRequest request;

    private QueryReferenceFactory factory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        factory = new QueryReferenceFactory(objectMapper);
    }

    @Test
    void create_ShouldReturnQueryReference_WhenValidInput() {
        // Given
        String queryCompletedJson = "{\"metadata\":{\"queryId\":\"test-query-id\"}}";
        String coordinatorUrl = "http://localhost:8080";

        when(request.getHeader(QueryReferenceFactory.COORDINATOR_CUSTOM_HEADER)).thenReturn(coordinatorUrl);

        // When
        QueryReference result = factory.create(queryCompletedJson, request);

        // Then
        assertEquals("test-query-id", result.queryId());
        assertEquals(coordinatorUrl, result.coordinatorUrl());
    }

    @Test
    void create_ShouldThrowException_WhenQueryIdIsMissing() {
        // Given
        String queryCompletedJson = "{\"metadata\":{}}";

        // When & Then
        assertThrows(InvalidQueryEventException.class, () -> factory.create(queryCompletedJson, request));
        
        // The header is not checked because extractQueryId fails first
    }

    @Test
    void create_ShouldThrowException_WhenQueryIdIsBlank() {
        // Given
        String queryCompletedJson = "{\"metadata\":{\"queryId\":\"\"}}";

        // When & Then
        assertThrows(InvalidQueryEventException.class, () -> factory.create(queryCompletedJson, request));
        
        // The header is not checked because extractQueryId fails first
    }

    @Test
    void create_ShouldThrowException_WhenJsonIsMalformed() {
        // Given
        String malformedJson = "invalid json";

        // When & Then
        assertThrows(InvalidQueryEventException.class, () -> factory.create(malformedJson, request));
        
        // The header is not checked because extractQueryId fails first
    }

    @Test
    void create_ShouldThrowException_WhenCoordinatorHeaderIsMissing() {
        // Given
        String queryCompletedJson = "{\"metadata\":{\"queryId\":\"test-query-id\"}}";

        when(request.getHeader(QueryReferenceFactory.COORDINATOR_CUSTOM_HEADER)).thenReturn(null);

        // When & Then
        assertThrows(InvalidQueryEventException.class, () -> factory.create(queryCompletedJson, request));
    }

    @Test
    void create_ShouldThrowException_WhenCoordinatorHeaderIsEmpty() {
        // Given
        String queryCompletedJson = "{\"metadata\":{\"queryId\":\"test-query-id\"}}";

        when(request.getHeader(QueryReferenceFactory.COORDINATOR_CUSTOM_HEADER)).thenReturn("");

        // When & Then
        assertThrows(InvalidQueryEventException.class, () -> factory.create(queryCompletedJson, request));
    }

    @Test
    void create_ShouldHandleComplexJsonStructure() {
        // Given
        String complexJson = """
                {
                    "metadata": {
                        "queryId": "complex-query-id",
                        "otherField": "value"
                    },
                    "statistics": {
                        "totalRows": 1000
                    }
                }
                """;
        String coordinatorUrl = "http://localhost:8080";

        when(request.getHeader(QueryReferenceFactory.COORDINATOR_CUSTOM_HEADER)).thenReturn(coordinatorUrl);

        // When
        QueryReference result = factory.create(complexJson, request);

        // Then
        assertEquals("complex-query-id", result.queryId());
        assertEquals(coordinatorUrl, result.coordinatorUrl());
    }

    @Test
    void create_ShouldHandleNestedMetadataStructure() {
        // Given
        String nestedJson = """
                {
                    "metadata": {
                        "queryId": "nested-query-id",
                        "session": {
                            "user": "testuser"
                        }
                    }
                }
                """;
        String coordinatorUrl = "http://localhost:8080";

        when(request.getHeader(QueryReferenceFactory.COORDINATOR_CUSTOM_HEADER)).thenReturn(coordinatorUrl);

        // When
        QueryReference result = factory.create(nestedJson, request);

        // Then
        assertEquals("nested-query-id", result.queryId());
        assertEquals(coordinatorUrl, result.coordinatorUrl());
    }
}
