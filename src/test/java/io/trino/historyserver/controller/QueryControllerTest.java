package io.trino.historyserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.trino.historyserver.common.GlobalProperties;
import io.trino.historyserver.dto.QueryReference;
import io.trino.historyserver.dto.QueryReferenceFactory;
import io.trino.historyserver.exception.QueryException;
import io.trino.historyserver.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QueryControllerTest {

    @Mock
    private QueryService queryService;

    @Mock
    private QueryReferenceFactory queryReferenceFactory;

    @Mock
    private GlobalProperties globalProperties;

    @InjectMocks
    private QueryController queryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(queryController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createQuery_ShouldReturnSuccessMessage_WhenValidRequest() throws Exception {
        // Given
        String queryCompletedJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";
        QueryReference queryRef = new QueryReference("test-query-id", "http://localhost:8080");
        
        when(queryReferenceFactory.create(anyString(), any())).thenReturn(queryRef);
        when(globalProperties.getEnvironment()).thenReturn("test");
        doNothing().when(queryService).createQuery(any(QueryReference.class), anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queryCompletedJson)
                .header("X-Trino-Coordinator-Url", "http://localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(content().string("Query test-query-id was successfully created."));

        verify(queryReferenceFactory).create(eq(queryCompletedJson), any());
        verify(queryService).createQuery(eq(queryRef), eq("test"));
    }

    @Test
    void createQuery_ShouldHandleServiceException() throws Exception {
        // Given
        String queryCompletedJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";
        QueryReference queryRef = new QueryReference("test-query-id", "http://localhost:8080");
        
        when(queryReferenceFactory.create(anyString(), any())).thenReturn(queryRef);
        when(globalProperties.getEnvironment()).thenReturn("test");
        doThrow(new QueryException("Service error", "test-query-id")).when(queryService).createQuery(any(QueryReference.class), anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queryCompletedJson)
                .header("X-Trino-Coordinator-Url", "http://localhost:8080"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getQuery_ShouldReturnQueryJson_WhenQueryExists() throws Exception {
        // Given
        String queryId = "test-query-id";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";
        
        when(globalProperties.getEnvironment()).thenReturn("test");
        when(queryService.getQuery(queryId, "test")).thenReturn(expectedQueryJson);

        // When & Then
        mockMvc.perform(get("/api/v1/query/{queryId}", queryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedQueryJson));

        verify(queryService).getQuery(queryId, "test");
    }

    @Test
    void getQuery_ShouldHandleServiceException() throws Exception {
        // Given
        String queryId = "test-query-id";
        
        when(globalProperties.getEnvironment()).thenReturn("test");
        when(queryService.getQuery(queryId, "test")).thenThrow(new QueryException("Query not found", queryId));

        // When & Then
        mockMvc.perform(get("/api/v1/query/{queryId}", queryId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void handleBaseQueryPath_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/query"))
                .andExpect(status().isNotFound());
    }
}
