<h1 align="center">Trino Query History Server</h1>
<p align="center">
    <img alt="Trino Logo" src="assets/trino-history.png" /></a>
</p>
<p align="center">
    <b>A backend service for persisting and serving query history from Trino coordinators.</b>
</p>

## Overview

The **Trino History Server** is a Spring Boot-based backend service that collects and stores query data from Trino coordinators.

When a Trino coordinator emits a [`QueryCompletedEvent`](https://trino.io/docs/475/admin/event-listeners-http.html#configuration-properties),
it can be sent to the History Server via the [HTTP Event Listener](https://trino.io/docs/475/admin/event-listeners-http.html) mechanism.

The History Server exposes the following endpoint for this purpose:

```
POST /api/v1/query
```

This endpoint expects:
* A [`QueryCompletedEvent`](https://trino.io/docs/475/admin/event-listeners-http.html#configuration-properties) JSON payload
* An `X-Trino-Coordinator-Url` [custom HTTP header](https://trino.io/docs/475/admin/event-listeners-http.html#custom-http-headers) identifying the source coordinator

Upon receiving the event, the server uses the coordinator URL to fetch the query JSON representation, and persists the document in a configurable storage.
This design decouples data collection from Trino's runtime, enabling long-term query retention and historical browsing via the [custom Trino UI frontend](https://github.com/yardenc2003/trino/tree/trino-history-server-475.1).

## API Endpoints

### Query Management

- **POST** `/api/v1/query` - Create a new query record from a QueryCompletedEvent
- **GET** `/api/v1/query/{queryId}` - Retrieve a specific query by ID

### Health Check

- **GET** `/actuator/health` - Application health status

## Architecture

### Query Flow 

```text
       ┌────────────────────┐
       │   Trino Coordinator│
       │(via Event Listener)│
       └────────┬───────────┘
                │ POST /api/v1/query/{queryId}
                ▼
      ┌────────────────────────┐
      │  History Server (Spring│
      │       Boot App)        │
      └────────┬───────────────┘
               │
               ├─ Fetch Query JSON
               │  from the coordinator using
               │  `X-Trino-Coordinator-Url` header
               │
               └─ Persist files to configurable storage
```

### User Access Flow

```text
        ┌─────────────┐
        │     User    │
        └─────┬───────┘
              │ queryId
              ▼
    ┌────────────────────────┐
    │ Forked Trino Web UI    │
    └────────┬───────────────┘
             │
             │ Requests query details via REST API
             │ to the History Server backend
             ▼
    ┌────────────────────────┐
    │ History Server (Spring │
    │ Boot App)              │
    └────────────────────────┘

```

## Configuration

The History Server can be configured via `application.properties` with the following key settings:

```properties
# Server settings
server.port=8080                          # Port on which the History Server runs

# Environment info
global.environment=production             # Server environment name (e.g., test, prod)

# Trino authentication for coordinator requests
# Note: This must be an admin user, as it is used to fetch all query data (across all users) from the coordinators
trino.auth.username=your-trino-username   # Username used when fetching query data from Trino coordinators
trino.auth.password=your-trino-password   # Password used when fetching query data from Trino coordinators

# Storage-retry settings (for all storage implementations)
storage.retry.max-retries=3       # Maximum retry attempts for failed storage operations
storage.retry.backoff-millis=500  # Time to wait (in milliseconds) between retry attempts

# Storage backend type (choose one)
storage.type=jdbc                         # Storage backend type: 'jdbc', 'filesystem', or 's3'

# JDBC storage-specific settings (for 'jdbc' backend)
storage.jdbc.dialect=postgresql  # SQL dialect for JDBC (e.g., postgresql, mysql)
storage.jdbc.url=jdbc:postgresql://db.example.com:5432/trino  # JDBC connection URL
storage.jdbc.username=your-db-username   # Database username
storage.jdbc.password=your-db-password   # Database password

# Filesystem storage-specific settings (for 'filesystem' backend)
storage.filesystem.query-dir=/var/data/trino-history/query  # Directory path to store query JSON files

# S3 storage-specific settings (for 's3' backend)
storage.s3.query-dir=query           # Directory (prefix) in the S3 bucket to store query files
storage.s3.storage-class=STANDARD    # S3 storage class (e.g., STANDARD, STANDARD_IA)
storage.s3.bucket=history            # S3 bucket name
storage.s3.region=us-east-1          # S3 bucket region
storage.s3.endpoint=https://s3.example.com   # S3 endpoint
storage.s3.access-key=your-access-key     # S3 access key
storage.s3.secret-key=your-secret-key     # S3 secret key
storage.s3.path-style-access=true         # Use path-style access

```

## Development

This project is developed using Java 24 and the Spring Boot framework, with Maven as the build tool (via the Maven Wrapper).

### Prerequisites

* Java 24+

### Build the Project

From the **root directory** of the project, run:

```bash
./mvnw clean install
```

### Run Locally

```bash
./mvnw spring-boot:run
```

By default, the History Server starts on port `8080`. Configuration — such as storage settings or Trino user authentication details — 
can be adjusted via `application.properties` or environment variables.

### Run with JAR

After building the project, you can run the JAR file directly:

```bash
java -jar target/trino-history-server-1.0.0.jar
```

### Environment Variables

You can override configuration using environment variables:

```bash
export TRINO_AUTH_USERNAME=your-username
export TRINO_AUTH_PASSWORD=your-password
export STORAGE_TYPE=filesystem
export STORAGE_FILESYSTEM_QUERY_DIR=/path/to/queries
export SERVER_PORT=8080
export GLOBAL_ENVIRONMENT=production

java -jar target/trino-history-server-1.0.0.jar
```

### Configuration Examples

#### Development Configuration
```properties
server.port=8080
global.environment=development
storage.type=filesystem
storage.filesystem.query-dir=./data/queries
trino.auth.username=admin
trino.auth.password=admin
logging.level.io.trino.historyserver=DEBUG
```

#### Production Configuration
```properties
server.port=8080
global.environment=production
storage.type=jdbc
storage.jdbc.dialect=postgresql
storage.jdbc.url=jdbc:postgresql://db.example.com:5432/trino_history
storage.jdbc.username=${DB_USERNAME}
storage.jdbc.password=${DB_PASSWORD}
trino.auth.username=${TRINO_USERNAME}
trino.auth.password=${TRINO_PASSWORD}
storage.retry.max-retries=5
storage.retry.backoff-millis=1000
```

## Building the Docker Image

To build a Docker image for the History Server backend, run:

```bash
./docker/build.sh [version] [arch]
```

* `[version]` (optional): The image version tag (e.g. `1.0.1`). Defaults to `latest` if not specified.
* `[arch]` (optional): The target architecture (e.g. `amd64`, `arm64`). Defaults to `amd64`.

Examples:

```bash
./docker/build.sh                # Builds version "latest" for amd64
```

```bash
./docker/build.sh 1.0.0         # Builds version 1.0.0 for amd64
```

```bash
./docker/build.sh 1.0.0 arm64    # Builds version 1.0.0 for arm64
```

This produces a Docker image tagged as:

```text
trino-history-backend:<version>-<arch>
```

## Kubernetes Deployment

The project includes a Helm chart for deploying the Trino History Server to Kubernetes.

### Prerequisites

- Kubernetes cluster
- Helm 3.x
- Docker image built and pushed to a registry

### Deploy with Helm

1. **Update Helm dependencies** (if any):
   ```bash
   helm dependency update helm/trino-history-server
   ```

2. **Deploy the application**:
   ```bash
   helm install trino-history-server helm/trino-history-server \
     --set image.repository=your-registry/trino-history-backend \
     --set image.tag=1.0.0 \
     --set trino.auth.username=your-username \
     --set trino.auth.password=your-password \
     --set storage.type=jdbc \
     --set storage.jdbc.url=jdbc:postgresql://db.example.com:5432/trino_history
   ```

3. **Check deployment status**:
   ```bash
   helm status trino-history-server
   kubectl get pods -l app.kubernetes.io/name=trino-history-server
   ```

### Helm Configuration

Key configuration values for the Helm chart:

```yaml
# Image configuration
image:
  repository: your-registry/trino-history-backend
  tag: "1.0.0"
  pullPolicy: IfNotPresent

# Replica configuration
replicaCount: 2

# Service configuration
service:
  type: ClusterIP
  port: 8080

# Trino authentication
trino:
  auth:
    username: "admin"
    password: "admin"

# Storage configuration
storage:
  type: "jdbc"  # or "filesystem" or "s3"
  jdbc:
    url: "jdbc:postgresql://db.example.com:5432/trino_history"
    username: "db_user"
    password: "db_password"
    dialect: "postgresql"

# Resource limits
resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi

# Autoscaling
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### Environment-Specific Deployments

#### Development
```bash
helm install trino-history-server-dev helm/trino-history-server \
  --namespace development \
  --set global.environment=development \
  --set storage.type=filesystem \
  --set storage.filesystem.queryDir=/data/queries \
  --set trino.auth.username=admin \
  --set trino.auth.password=admin
```

#### Production
```bash
helm install trino-history-server-prod helm/trino-history-server \
  --namespace production \
  --set global.environment=production \
  --set storage.type=jdbc \
  --set storage.jdbc.url=jdbc:postgresql://prod-db.example.com:5432/trino_history \
  --set storage.jdbc.username=${DB_USERNAME} \
  --set storage.jdbc.password=${DB_PASSWORD} \
  --set trino.auth.username=${TRINO_USERNAME} \
  --set trino.auth.password=${TRINO_PASSWORD} \
  --set replicaCount=3 \
  --set autoscaling.enabled=true
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Application Startup Failures

**Error: `trino.auth.username must be provided` and `trino.auth.password must be provided`**

```
APPLICATION FAILED TO START
***************************
Description:
Binding to target io.trino.historyserver.auth.TrinoAuthProperties$$SpringCGLIB$$0 failed:
    Property: trino.auth.username
    Value: "null"
    Reason: trino.auth.username must be provided
    Property: trino.auth.password
    Value: "null"
    Reason: trino.auth.password must be provided
```

**Solution:**
Ensure that the Trino authentication credentials are properly configured in your `application.properties`:

```properties
# Trino authentication settings
trino.auth.username=your-trino-username
trino.auth.password=your-trino-password
```

**Note:** These credentials must be for an admin user, as the History Server needs to fetch query data across all users from the Trino coordinators.

#### 2. Connection Issues with Trino Coordinators

**Error: `Connection refused` or `ConnectException`**

```
java.net.ConnectException: Connection refused
    at java.base/sun.nio.ch.Net.connect0(Native Method)
    at java.base/sun.nio.ch.Net.connect(Net.java:565)
    at java.base/sun.nio.ch.Net.connect(Net.java:554)
```

**Solutions:**
1. **Verify Trino Coordinator URL**: Ensure the `X-Trino-Coordinator-Url` header points to a valid, accessible Trino coordinator
2. **Check Network Connectivity**: Verify that the History Server can reach the Trino coordinator
3. **Validate Authentication**: Ensure the provided username/password are correct and have sufficient privileges
4. **Check Trino Coordinator Status**: Verify that the Trino coordinator is running and accepting connections

#### 3. Storage Backend Issues

**Error: `StorageInitializationException`**

```
event=init_storage_failed type=server_error message="..."
```

**Solutions:**

**For Filesystem Storage:**
```properties
storage.type=filesystem
storage.filesystem.query-dir=/path/to/query/directory
```
- Ensure the directory exists and is writable
- Check file system permissions

**For JDBC Storage:**
```properties
storage.type=jdbc
storage.jdbc.dialect=postgresql
storage.jdbc.url=jdbc:postgresql://host:port/database
storage.jdbc.username=db_user
storage.jdbc.password=db_password
```
- Verify database connectivity
- Ensure the database user has CREATE/INSERT/SELECT permissions
- Check that the required tables exist

**For S3 Storage:**
```properties
storage.type=s3
storage.s3.bucket=your-bucket-name
storage.s3.region=us-east-1
storage.s3.access-key=your-access-key
storage.s3.secret-key=your-secret-key
```
- Verify S3 credentials and permissions
- Ensure the bucket exists and is accessible
- Check network connectivity to S3

#### 4. Query Fetch Failures

**Error: `QueryFetchException`**

```
event=query_fetch_failed type=server_error queryId=... message="..."
```

**Solutions:**
1. **Check Query ID Format**: Ensure the query ID is valid and exists on the coordinator
2. **Verify Coordinator Access**: The History Server must be able to access the coordinator's query details endpoint
3. **Authentication Issues**: Ensure the provided credentials have access to query metadata
4. **Query Lifecycle**: The query might have been purged from the coordinator's memory

#### 5. Session Management Issues

**Error: `ExpiredSessionException` or authentication failures**

**Solutions:**
1. **Refresh Credentials**: Update the username/password if they have expired
2. **Check Session Timeout**: Trino sessions may have timeouts; consider implementing session refresh logic
3. **Verify User Permissions**: Ensure the user has the necessary permissions to access query data

### Debugging Tips

1. **Enable Debug Logging**: Add the following to `application.properties`:
   ```properties
   logging.level.io.trino.historyserver=DEBUG
   logging.level.org.springframework.web=DEBUG
   ```

2. **Check Application Health**: Use the health endpoint to verify application status:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Monitor Logs**: Watch the application logs for detailed error information and stack traces

4. **Test Connectivity**: Manually test connectivity to Trino coordinators:
   ```bash
   curl -u username:password http://trino-coordinator:8080/v1/query/query-id
   ```

### Performance Considerations

1. **Storage Backend Selection**:
   - **Filesystem**: Best for single-instance deployments
   - **JDBC**: Best for multi-instance deployments with shared database
   - **S3**: Best for cloud deployments with high availability requirements

2. **Retry Configuration**:
   ```properties
   storage.retry.max-retries=3
   storage.retry.backoff-millis=500
   ```

3. **Memory Usage**: Monitor JVM memory usage, especially when handling large query results

### Security Considerations

1. **Credential Management**: Store Trino credentials securely (use environment variables or secret management systems)
2. **Network Security**: Ensure secure communication between History Server and Trino coordinators
3. **Access Control**: Implement proper authentication and authorization for the History Server API endpoints
4. **Data Privacy**: Be aware that the History Server stores query data including potentially sensitive information

## Monitoring and Maintenance

### Health Checks

The application provides health check endpoints for monitoring:

- **Health Check**: `GET /actuator/health`
- **Info**: `GET /actuator/info` (if configured)

### Logging

The application uses structured logging with the following key events:

- `event=received_query_complete_event` - Query event received
- `event=create_query_succeeded` - Query successfully stored
- `event=received_query_read_event` - Query read request
- `event=get_query_succeeded` - Query successfully retrieved
- `event=invalid_query_event` - Invalid query event received
- `event=trino_auth_failed` - Trino authentication failure
- `event=query_fetch_failed` - Failed to fetch query from coordinator
- `event=query_storage_failed` - Failed to store query
- `event=init_storage_failed` - Storage initialization failure

### Metrics

Consider implementing application metrics for:

- Query processing rate
- Storage operation latency
- Error rates by type
- Trino coordinator connectivity status
- Storage backend health

### Maintenance Tasks

1. **Storage Cleanup**: Implement periodic cleanup of old query data based on retention policies
2. **Database Maintenance**: For JDBC storage, schedule regular database maintenance (VACUUM, ANALYZE)
3. **Log Rotation**: Configure log rotation to prevent disk space issues
4. **Backup**: Implement regular backups of stored query data
5. **Certificate Management**: Keep SSL certificates updated for secure communication

### Performance Tuning

1. **JVM Tuning**: Adjust JVM heap size based on query data volume
2. **Connection Pooling**: Configure appropriate connection pool sizes for database connections
3. **Batch Processing**: Consider implementing batch processing for high-volume scenarios
4. **Caching**: Implement caching for frequently accessed queries

## Contributing

### Development Setup

1. **Fork the repository**
2. **Clone your fork**:
   ```bash
   git clone https://github.com/your-username/trino-history-server.git
   cd trino-history-server
   ```

3. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Make your changes and test**:
   ```bash
   ./mvnw clean test
   ```

5. **Submit a pull request**

### Code Style

- Follow Java coding conventions
- Use meaningful variable and method names
- Add appropriate comments for complex logic
- Ensure all tests pass before submitting

### Testing

Run the test suite:

```bash
./mvnw test
```

Run integration tests:

```bash
./mvnw verify
```

## License

This project is licensed under the [MIT License](LICENSE).

## Support

For issues and questions:

1. Check the [troubleshooting section](#troubleshooting) above
2. Search existing [GitHub issues](https://github.com/your-org/trino-history-server/issues)
3. Create a new issue with detailed information about your problem
4. Include relevant logs and configuration details
