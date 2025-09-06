package io.trino.historyserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 (Swagger) configuration for Trino History Server
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trinoHistoryServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trino History Server API")
                        .description("""
                                A comprehensive history server for Trino queries that provides:
                                
                                - **Query Storage**: Store completed Trino queries with metadata
                                - **Query Retrieval**: Fetch stored queries by ID
                                - **Multiple Storage Backends**: Support for local filesystem and S3
                                - **Authentication**: Secure access to Trino coordinator
                                - **Retry Logic**: Robust error handling with configurable retries
                                
                                This API allows you to manage and retrieve historical Trino query data
                                for analysis, debugging, and monitoring purposes.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Trino History Server Team")
                                .email("support@trino.io")
                                .url("https://trino.io"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://trino-history.example.com")
                                .description("Production server")
                ));
    }
}
