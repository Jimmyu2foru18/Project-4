package com.webserver.integration;

import com.webserver.core.Client;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ServerContainerTest {
    @Container
    private static final GenericContainer<?> server = 
        new GenericContainer<>("your-server-image")
            .withExposedPorts(8080);
    
    @Test
    void testServerInContainer() {
        Client client = new Client(server.getHost(), server.getFirstMappedPort());
        String response = client.sendGetRequest("/");
        assertTrue(response.contains("200 OK"));
    }
} 