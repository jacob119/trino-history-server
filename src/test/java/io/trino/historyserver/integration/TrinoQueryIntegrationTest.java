package io.trino.historyserver.integration;

import io.trino.jdbc.TrinoConnection;
import io.trino.jdbc.TrinoDriver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that runs a real Trino 475 server and tests query execution
 * through the History Server API.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TrinoQueryIntegrationTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    // Trino 475 server container
    @Container
    static GenericContainer<?> trinoContainer = new GenericContainer<>("trinodb/trino:475")
            .withExposedPorts(8080)
            .withEnv("TRINO_ENVIRONMENT", "test")
            .waitingFor(Wait.forHttp("/v1/info")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3)))
            .withStartupTimeout(Duration.ofMinutes(5));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure Trino connection properties for the test
        String trinoHost = trinoContainer.getHost();
        Integer trinoPort = trinoContainer.getMappedPort(8080);
        String trinoUrl = String.format("http://%s:%d", trinoHost, trinoPort);
        
        registry.add("trino.auth.username", () -> "test");
        registry.add("trino.auth.password", () -> "test");
        registry.add("trino.coordinator.url", () -> trinoUrl);
        
        log.info("Trino server running at: {}", trinoUrl);
    }

    @BeforeAll
    static void setupTrino() throws Exception {
        // Wait for Trino to be fully ready
        trinoContainer.start();
        
        // Give Trino some time to fully initialize
        Thread.sleep(10000);
        
        log.info("Trino container started successfully");
        log.info("Trino UI available at: http://{}:{}", 
                trinoContainer.getHost(), trinoContainer.getMappedPort(8080));
    }

    @AfterAll
    static void tearDown() {
        if (trinoContainer.isRunning()) {
            trinoContainer.stop();
            log.info("Trino container stopped");
        }
    }

    @Test
    void testTrinoConnection() throws Exception {
        // Test direct connection to Trino
        String jdbcUrl = String.format("jdbc:trino://%s:%d", 
                trinoContainer.getHost(), trinoContainer.getMappedPort(8080));
        
        log.info("Connecting to Trino at: {}", jdbcUrl);
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "test", null)) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
            
            // Test a simple query
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1 as test_value")) {
                
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("test_value")).isEqualTo(1);
                
                log.info("Successfully executed test query on Trino");
            }
        }
    }

    @Test
    void testQueryExecutionAndHistoryStorage() throws Exception {
        // Execute a query on Trino
        String queryId = executeTestQuery();
        
        // Wait a moment for query to complete
        Thread.sleep(2000);
        
        // Create a mock query completion event
        String queryCompletedEvent = createMockQueryCompletedEvent(queryId);
        
        // Send the query completion event to History Server
        String historyServerUrl = "http://localhost:" + port + "/api/v1/query";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Trino-Coordinator-Url", "http://localhost:" + trinoContainer.getMappedPort(8080));
        HttpEntity<String> request = new HttpEntity<>(queryCompletedEvent, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                historyServerUrl, 
                HttpMethod.POST, 
                request, 
                String.class
        );
        
        // Verify the response
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("successfully created");
        
        log.info("Query completion event sent to History Server: {}", response.getBody());
        
        // Retrieve the stored query from History Server
        String getQueryUrl = "http://localhost:" + port + "/api/v1/query/" + queryId;
        ResponseEntity<String> getResponse = restTemplate.getForEntity(getQueryUrl, String.class);
        
        // Verify the stored query can be retrieved
        assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getResponse.getBody()).contains(queryId);
        
        log.info("Successfully retrieved stored query from History Server");
    }

    private String executeTestQuery() throws Exception {
        String jdbcUrl = String.format("jdbc:trino://%s:%d", 
                trinoContainer.getHost(), trinoContainer.getMappedPort(8080));
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "test", null);
             Statement statement = connection.createStatement()) {
            
            // Execute a simple query that will generate a query ID
            String sql = "SELECT 1 AS test_value, 'integration_test' AS test_type";
            
            log.info("Executing query: {}", sql);
            
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                assertThat(resultSet.next()).isTrue();
                
                // Try to get query ID from statement metadata
                try {
                    // Check if we can get query ID from the statement
                    if (statement instanceof io.trino.jdbc.TrinoStatement) {
                        io.trino.jdbc.TrinoStatement trinoStatement = (io.trino.jdbc.TrinoStatement) statement;
                        // Try to access query ID through reflection or other means
                        log.info("Query executed successfully with TrinoStatement");
                    }
                } catch (Exception e) {
                    log.warn("Could not extract query ID from statement: {}", e.getMessage());
                }
            }
        }
        
        // Generate a mock query ID for testing
        String queryId = "test_query_" + System.currentTimeMillis();
        log.info("Using mock query ID: {}", queryId);
        return queryId;
    }

    private String createMockQueryCompletedEvent(String queryId) {
        return String.format("""
                {
                  "metadata": {
                    "queryId": "%s",
                    "query": "SELECT 1 AS test_value, 'integration_test' AS test_type",
                    "state": "FINISHED",
                    "createTime": "%s",
                    "endTime": "%s"
                  },
                  "queryStats": {
                    "totalSplits": 1,
                    "queuedSplits": 0,
                    "runningSplits": 0,
                    "completedSplits": 1,
                    "totalCpuTime": "1.00s",
                    "totalBlockedTime": "0.00s"
                  },
                  "session": {
                    "user": "test",
                    "source": "integration-test"
                  },
                  "catalog": "system",
                  "schema": "information_schema"
                }
                """, 
                queryId,
                java.time.Instant.now().minusSeconds(10).toString(),
                java.time.Instant.now().toString()
        );
    }

    @Test
    void testTrinoServerInfo() throws Exception {
        // Test that Trino server is responding to info endpoint
        String trinoHost = trinoContainer.getHost();
        Integer trinoPort = trinoContainer.getMappedPort(8080);
        String infoUrl = String.format("http://%s:%d/v1/info", trinoHost, trinoPort);
        
        ResponseEntity<Map> response = restTemplate.getForEntity(infoUrl, Map.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        
        log.info("Trino server info: {}", response.getBody());
    }
}
