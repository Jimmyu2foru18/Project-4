package com.webserver;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private static final int PORT = 8080;
    private Server server;
    private Client client;
    private Thread serverThread;

    @BeforeEach
    void setUp() {
        server = new Server(PORT, 10);
        serverThread = new Thread(server::start);
        serverThread.start();
        client = new Client("localhost", PORT);
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
    }

    @Test
    void basicRequestTest() throws IOException {
        String response = client.sendGetRequest("/");
        assertNotNull(response);
        assertTrue(response.contains("200 OK"));
    }

    @Test
    void concurrentRequestsTest() throws InterruptedException {
        int numClients = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        CountDownLatch latch = new CountDownLatch(numClients);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numClients; i++) {
            executor.submit(() -> {
                try {
                    String response = client.sendGetRequest("/");
                    if (response.contains("200 OK")) {
                        successCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertEquals(numClients, successCount.get());
    }

    @Test
    void errorHandlingTest() throws IOException {
        String response = client.sendGetRequest("/nonexistent");
        assertTrue(response.contains("404"));
    }

    @Test
    void performanceTest() throws IOException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            client.sendGetRequest("/");
        }
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration / 100 < 100, "Average response time should be under 100ms");
    }
} 