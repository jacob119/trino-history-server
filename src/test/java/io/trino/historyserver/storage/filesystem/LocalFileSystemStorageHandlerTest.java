package io.trino.historyserver.storage.filesystem;

import io.trino.historyserver.exception.QueryStorageException;
import io.trino.historyserver.exception.StorageInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalFileSystemStorageHandlerTest {

    @Mock
    private FileSystemStorageHandlerProperties properties;

    private LocalFileSystemStorageHandler storageHandler;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageHandler = new LocalFileSystemStorageHandler(properties);
    }

    @Test
    void ensureDirectoryExists_ShouldCreateDirectory_WhenDirectoryDoesNotExist() {
        // Given
        String queryDir = tempDir.resolve("queries").toString();
        when(properties.getQueryDir()).thenReturn(queryDir);

        // When
        storageHandler.ensureDirectoryExists();

        // Then
        assertTrue(Files.exists(Path.of(queryDir)));
        assertTrue(Files.isDirectory(Path.of(queryDir)));
    }

    @Test
    void ensureDirectoryExists_ShouldNotThrowException_WhenDirectoryAlreadyExists() {
        // Given
        String queryDir = tempDir.toString();
        when(properties.getQueryDir()).thenReturn(queryDir);

        // When & Then
        assertDoesNotThrow(() -> storageHandler.ensureDirectoryExists());
    }

    @Test
    void ensureDirectoryExists_ShouldThrowException_WhenDirectoryCreationFails() {
        // Given
        String invalidPath = "/invalid/path/that/does/not/exist/and/cannot/be/created";
        when(properties.getQueryDir()).thenReturn(invalidPath);

        // When & Then
        assertThrows(StorageInitializationException.class, () -> storageHandler.ensureDirectoryExists());
    }

    @Test
    void writeQuery_ShouldWriteQueryToFile_WhenValidInput() throws Exception {
        // Given
        String queryDir = tempDir.toString();
        String queryId = "test-query-id";
        String environment = "test";
        String queryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(properties.getQueryDir()).thenReturn(queryDir);
        storageHandler.ensureDirectoryExists();

        // When
        assertDoesNotThrow(() -> storageHandler.writeQuery(queryId, environment, queryJson));

        // Then
        Path expectedPath = Path.of(queryDir, queryId + ".json");
        assertTrue(Files.exists(expectedPath));
        assertEquals(queryJson, Files.readString(expectedPath));
    }

    @Test
    void writeQuery_ShouldOverwriteExistingFile_WhenFileAlreadyExists() throws Exception {
        // Given
        String queryDir = tempDir.toString();
        String queryId = "test-query-id";
        String environment = "test";
        String originalJson = "{\"queryId\":\"test-query-id\",\"state\":\"RUNNING\"}";
        String updatedJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(properties.getQueryDir()).thenReturn(queryDir);
        storageHandler.ensureDirectoryExists();

        // Write original file
        storageHandler.writeQuery(queryId, environment, originalJson);

        // When - Write updated content
        storageHandler.writeQuery(queryId, environment, updatedJson);

        // Then
        Path expectedPath = Path.of(queryDir, queryId + ".json");
        assertEquals(updatedJson, Files.readString(expectedPath));
    }

    @Test
    void readQuery_ShouldReadQueryFromFile_WhenFileExists() throws Exception {
        // Given
        String queryDir = tempDir.toString();
        String queryId = "test-query-id";
        String environment = "test";
        String expectedQueryJson = "{\"queryId\":\"test-query-id\",\"state\":\"FINISHED\"}";

        when(properties.getQueryDir()).thenReturn(queryDir);
        storageHandler.ensureDirectoryExists();

        // Write file first
        storageHandler.writeQuery(queryId, environment, expectedQueryJson);

        // When
        String result = storageHandler.readQuery(queryId, environment);

        // Then
        assertEquals(expectedQueryJson, result);
    }

    @Test
    void readQuery_ShouldThrowException_WhenFileDoesNotExist() {
        // Given
        String queryDir = tempDir.toString();
        String queryId = "non-existent-query";
        String environment = "test";

        when(properties.getQueryDir()).thenReturn(queryDir);
        storageHandler.ensureDirectoryExists();

        // When & Then
        assertThrows(QueryStorageException.class, () -> storageHandler.readQuery(queryId, environment));
    }

    @Test
    void getQueryPath_ShouldReturnCorrectPath() {
        // Given
        String queryDir = "/test/queries";
        String queryId = "test-query-id";
        String expectedPath = "/test/queries/test-query-id.json";

        when(properties.getQueryDir()).thenReturn(queryDir);

        // When
        Path result = storageHandler.getQueryPath(queryId);

        // Then
        assertEquals(expectedPath, result.toString());
    }

    @Test
    void writeQuery_ShouldHandleSpecialCharactersInQueryId() throws Exception {
        // Given
        String queryDir = tempDir.toString();
        String queryId = "test-query-with-special-chars_123";
        String environment = "test";
        String queryJson = "{\"queryId\":\"test-query-with-special-chars_123\",\"state\":\"FINISHED\"}";

        when(properties.getQueryDir()).thenReturn(queryDir);
        storageHandler.ensureDirectoryExists();

        // When
        assertDoesNotThrow(() -> storageHandler.writeQuery(queryId, environment, queryJson));

        // Then
        Path expectedPath = Path.of(queryDir, queryId + ".json");
        assertTrue(Files.exists(expectedPath));
        assertEquals(queryJson, Files.readString(expectedPath));
    }

    @Test
    void writeQuery_ShouldHandleLargeQueryJson() throws Exception {
        // Given
        String queryDir = tempDir.toString();
        String queryId = "large-query";
        String environment = "test";
        StringBuilder largeJson = new StringBuilder("{\"queryId\":\"large-query\",\"state\":\"FINISHED\",\"data\":\"");
        
        // Create a large JSON string
        for (int i = 0; i < 1000; i++) {
            largeJson.append("This is a large query data string. ");
        }
        largeJson.append("\"}");

        when(properties.getQueryDir()).thenReturn(queryDir);
        storageHandler.ensureDirectoryExists();

        // When
        assertDoesNotThrow(() -> storageHandler.writeQuery(queryId, environment, largeJson.toString()));

        // Then
        Path expectedPath = Path.of(queryDir, queryId + ".json");
        assertTrue(Files.exists(expectedPath));
        assertEquals(largeJson.toString(), Files.readString(expectedPath));
    }
}
