package io.trino.historyserver.storage.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.trino.historyserver.exception.QueryStorageException;
import io.trino.historyserver.exception.StorageInitializationException;
import io.trino.historyserver.storage.QueryStorageHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "filesystem", matchIfMissing = true)
@ConfigurationProperties(prefix = "storage.filesystem")
@RequiredArgsConstructor
public class LocalFileSystemStorageHandler
        implements QueryStorageHandler
{
    private static final String FILE_EXTENSION = ".json";

    private final FileSystemStorageHandlerProperties props;

    @PostConstruct
    public void ensureDirectoryExists()
    {
        String queryDir = props.getQueryDir();

        try {
            Files.createDirectories(Path.of(queryDir));
        }
        catch (IOException e) {
            throw new StorageInitializationException(
                    String.format(
                            "Failed to create directory \"%s\" existence due to filesystem error.",
                            queryDir
                    ), e
            );
        }
        log.info("event=directory_create_succeeded type=success path=\"{}\"", queryDir);
    }

    @Override
    public void writeQuery(String queryId, String environment, String queryJson)
            throws QueryStorageException
    {
        Path path = getQueryPath(queryId);

        try {
            this.write(path, queryJson);
        }
        catch (IOException e) {
            throw new QueryStorageException(
                    String.format(
                            "Failed to write query %s JSON to path \"%s\".",
                            queryId, path
                    ),
                    queryId, e
            );
        }
        log.info("event=query_write_succeeded type=success queryId={} path=\"{}\"", queryId, path);
    }

    @Override
    public String readQuery(String queryId, String environment)
            throws QueryStorageException
    {
        String queryJson;
        Path path = getQueryPath(queryId);

        try {
            queryJson = this.read(path);
        }
        catch (IOException e) {
            throw new QueryStorageException(
                    String.format(
                            "Failed to read query %s JSON from path \"%s\".",
                            queryId, path
                    ),
                    queryId, e
            );
        }
        log.info("event=query_read_succeeded type=success queryId={} path=\"{}\"", queryId, path);
        return queryJson;
    }

    private void write(Path fullPath, String content)
            throws IOException
    {
        Files.writeString(fullPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String read(Path fullPath)
            throws IOException
    {
        return Files.readString(fullPath);
    }

    public Path getQueryPath(String queryId)
    {
        return Path.of(props.getQueryDir(), queryId + FILE_EXTENSION);
    }
}
