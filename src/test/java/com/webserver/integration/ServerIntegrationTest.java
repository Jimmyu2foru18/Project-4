package com.webserver.integration;

import com.webserver.core.Server;
import com.webserver.core.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ServerIntegrationTest {
    private static final int PORT = 8083;
    private static final int THREAD_POOL_SIZE = 10;
    private Server server;
    private Thread serverThread;
    private static final String TEST_CONTENT = "<html><body>Test Page</body></html>";

    @BeforeEach
    void setUp() throws IOException {
        // Create test files
        setupTestFiles();
        
        // Start server
        server = new Server(PORT, THREAD_POOL_SIZE);
        serverThread = new Thread(server::start);
        serverThread.start();
        
        // Wait for server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
        server.stop();
        try {
            serverThread.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cleanupTestFiles();
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        int numClients = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        List<Future<String>> futures = new ArrayList<>();

        // Submit concurrent requests
        for (int i = 0; i < numClients; i++) {
            futures.add(executor.submit(() -> {
                Client client = new Client("localhost", PORT);
                return client.sendGetRequest("/test.html");
            }));
        }

        // Verify all responses
        int successCount = 0;
        for (Future<String> future : futures) {
            try {
                String response = future.get(5, TimeUnit.SECONDS);
                if (response.contains("200 OK") && response.contains(TEST_CONTENT)) {
                    successCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Executor should terminate");
        assertEquals(numClients, successCount, "All requests should succeed");
    }

    @Test
    void testErrorHandling() throws IOException {
        Client client = new Client("localhost", PORT);
        
        // Test 404 Not Found
        String response = client.sendGetRequest("/nonexistent.html");
        assertTrue(response.contains("404 Not Found"), "Should return 404 for nonexistent file");

        // Test 400 Bad Request (malformed path)
        response = client.sendGetRequest("/../../../etc/passwd");
        assertTrue(response.contains("400 Bad Request"), "Should return 400 for malformed path");
    }

    @Test
    void testPerformance() throws IOException {
        Client client = new Client("localhost", PORT);
        long startTime = System.currentTimeMillis();
        
        // Make 100 sequential requests
        for (int i = 0; i < 100; i++) {
            String response = client.sendGetRequest("/test.html");
            assertTrue(response.contains("200 OK"));
        }
        
        long endTime = System.currentTimeMillis();
        long averageResponseTime = (endTime - startTime) / 100;
        
        // Check if average response time is under 100ms
        assertTrue(averageResponseTime < 100, 
            "Average response time should be under 100ms but was " + averageResponseTime + "ms");
    }

    private void setupTestFiles() throws IOException {
        Path publicDir = Paths.get("src/main/resources/public");
        Files.createDirectories(publicDir);
        Files.write(publicDir.resolve("test.html"), 
                    TEST_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private void cleanupTestFiles() {
        try {
            Files.deleteIfExists(Paths.get("src/main/resources/public/test.html"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 