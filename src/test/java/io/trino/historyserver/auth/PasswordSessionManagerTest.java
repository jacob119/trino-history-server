package io.trino.historyserver.auth;

import io.trino.historyserver.exception.TrinoAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordSessionManagerTest {

    @Mock
    private TrinoAuthProperties authProps;

    @Mock
    private WebClient webClient;

    private PasswordSessionManager passwordSessionManager;

    @BeforeEach
    void setUp() {
        passwordSessionManager = new PasswordSessionManager(authProps, webClient);
    }

    @Test
    void constructor_ShouldInitializeWithProperties() {
        // Given
        TrinoAuthProperties testProps = mock(TrinoAuthProperties.class);
        WebClient testWebClient = mock(WebClient.class);

        // When
        PasswordSessionManager manager = new PasswordSessionManager(testProps, testWebClient);

        // Then
        assertNotNull(manager);
    }

    @Test
    void getSessionCookie_ShouldThrowException_WhenWebClientFails() {
        // Given
        String coordinatorUrl = "http://localhost:8080";

        when(webClient.post()).thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> passwordSessionManager.getSessionCookie(coordinatorUrl));
    }

    @Test
    void refreshSessionCookie_ShouldCallWebClient() {
        // Given
        String coordinatorUrl = "http://localhost:8080";

        when(webClient.post()).thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> passwordSessionManager.refreshSessionCookie(coordinatorUrl));
    }

    @Test
    void getSessionCookie_ShouldHandleMultipleCoordinators() {
        // Given
        String coordinator1 = "http://coordinator1:8080";
        String coordinator2 = "http://coordinator2:8080";

        when(webClient.post()).thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> passwordSessionManager.getSessionCookie(coordinator1));
        assertThrows(RuntimeException.class, () -> passwordSessionManager.getSessionCookie(coordinator2));
    }
}
