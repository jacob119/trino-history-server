package io.trino.historyserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trino.historyserver.common.GlobalProperties;
import io.trino.historyserver.dto.QueryReference;
import io.trino.historyserver.service.QueryService;
import io.trino.historyserver.dto.QueryReferenceFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/query")
@AllArgsConstructor
@Tag(name = "Query Management", description = "APIs for managing Trino query history")
public class QueryController
{
    private final QueryService queryService;
    private final QueryReferenceFactory queryReferenceFactory;
    private final GlobalProperties globalProps;

    @PostMapping
    @Operation(
            summary = "Create a new query record",
            description = "Stores a completed Trino query event in the history server. " +
                         "The query data should be in JSON format as received from Trino's query completion event."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Query successfully created",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    value = "Query 20231201_123456_00001_abcde was successfully created."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid query event data",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during query creation",
                    content = @Content(mediaType = "text/plain")
            )
    })
    public String createQuery(
            @Parameter(
                    description = "JSON string containing the completed query event data from Trino",
                    required = true,
                    example = """
                            {
                              "queryId": "20231201_123456_00001_abcde",
                              "query": "SELECT * FROM my_table LIMIT 10",
                              "state": "FINISHED",
                              "createTime": "2023-12-01T12:34:56.789Z",
                              "endTime": "2023-12-01T12:34:57.123Z"
                            }
                            """
            )
            @RequestBody String queryCompletedJson, 
            HttpServletRequest request)
    {
        QueryReference queryRef = queryReferenceFactory.create(queryCompletedJson, request);

        log.info("event=received_query_complete_event queryId={} coordinator={}",
                queryRef.queryId(),
                queryRef.coordinatorUrl());

        queryService.createQuery(queryRef, globalProps.getEnvironment());
        log.info("event=create_query_succeeded queryId={}", queryRef.queryId());

        return String.format(
                "Query %s was successfully created.",
                queryRef.queryId()
        );
    }

    @GetMapping
    @Operation(
            summary = "Handle base query path",
            description = "Returns 404 for the base query path as it's not a valid endpoint"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Not Found - Base query path is not a valid endpoint"
    )
    public ResponseEntity<Void> handleBaseQueryPath()
    {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{queryId}")
    @Operation(
            summary = "Retrieve a query by ID",
            description = "Fetches a stored Trino query by its unique identifier. " +
                         "Returns the complete query data in JSON format."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Query found and returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "queryId": "20231201_123456_00001_abcde",
                                              "query": "SELECT * FROM my_table LIMIT 10",
                                              "state": "FINISHED",
                                              "createTime": "2023-12-01T12:34:56.789Z",
                                              "endTime": "2023-12-01T12:34:57.123Z",
                                              "queryStats": {
                                                "totalSplits": 10,
                                                "queuedSplits": 0,
                                                "runningSplits": 0,
                                                "completedSplits": 10
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Query not found",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during query retrieval",
                    content = @Content(mediaType = "text/plain")
            )
    })
    public ResponseEntity<String> getQuery(
            @Parameter(
                    description = "Unique identifier of the query to retrieve",
                    required = true,
                    example = "20231201_123456_00001_abcde"
            )
            @PathVariable String queryId)
    {
        log.info("event=received_query_read_event queryId={}", queryId);

        String queryJson = queryService.getQuery(queryId, globalProps.getEnvironment());
        log.info("event=get_query_succeeded queryId={}", queryId);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(queryJson);
    }
}
