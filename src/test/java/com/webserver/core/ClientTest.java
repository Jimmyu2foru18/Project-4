package com.webserver.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

class ClientTest {
    private Server server;
    private Client client;
    private static final int TEST_PORT = 8082;
    private Thread serverThread;

    @BeforeEach
    void setUp() {
        server = new Server(TEST_PORT, 2);
        serverThread = new Thread(server::start);
        serverThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client = new Client("localhost", TEST_PORT);
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
        try {
            serverThread.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testClientConnection() {
        try {
            client.connect();
            assertTrue(true, "Connection successful");
        } catch (IOException e) {
            fail("Failed to connect to server: " + e.getMessage());
        }
    }

    @Test
    void testSendGetRequest() {
        try {
            String response = client.sendGetRequest("/");
            assertNotNull(response, "Response should not be null");
            assertTrue(response.contains("HTTP/1.0"), "Response should contain HTTP/1.0");
            assertTrue(response.contains("200 OK") || response.contains("404 Not Found"), 
                "Response should contain valid status code");
        } catch (IOException e) {
            fail("Failed to send GET request: " + e.getMessage());
        }
    }
} 