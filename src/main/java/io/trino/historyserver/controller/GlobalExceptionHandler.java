package io.trino.historyserver.controller;

import io.trino.historyserver.exception.InvalidQueryEventException;
import io.trino.historyserver.exception.QueryException;
import io.trino.historyserver.exception.QueryFetchException;
import io.trino.historyserver.exception.QueryStorageException;
import io.trino.historyserver.exception.StorageInitializationException;
import io.trino.historyserver.exception.TrinoAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidQueryEventException.class)
    public ResponseEntity<String> handleInvalidEventError(InvalidQueryEventException e) {
        log.error("event=invalid_query_event type=client_error message=\"{}\"", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Received invalid query event: " + e.getMessage());
    }

    @ExceptionHandler(TrinoAuthException.class)
    public ResponseEntity<String> handleTrinoAuthError(TrinoAuthException e) {
        log.error("event=trino_auth_failed type=server_error message=\"{}\"", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to authenticate with the coordinator: " + e.getMessage());
    }

    @ExceptionHandler(QueryFetchException.class)
    public ResponseEntity<String> handleFetchError(QueryFetchException e) {
        log.error("event=query_fetch_failed type=server_error queryId={} message=\"{}\"", e.getQueryId(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch query from coordinator: " + e.getMessage());
    }

    @ExceptionHandler(QueryStorageException.class)
    public ResponseEntity<String> handleStorageError(QueryStorageException e) {
        log.error("event=query_storage_failed type=server_error queryId={} message=\"{}\"", e.getQueryId(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error handling query file: " + e.getMessage());
    }

    @ExceptionHandler(StorageInitializationException.class)
    public ResponseEntity<String> handleStorageInitError(StorageInitializationException e) {
        log.error("event=init_storage_failed type=server_error message=\"{}\"", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error initializing storage: " + e.getMessage());
    }

    @ExceptionHandler(QueryException.class)
    public ResponseEntity<String> handleQueryError(QueryException e) {
        log.error("event=query_error type=server_error queryId={} message=\"{}\"", e.getQueryId(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Query operation failed: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericError(Exception e) {
        log.error("event=unexpected_exception type=server_error message=\"{}\"", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong: " + e.getMessage());
    }
}
