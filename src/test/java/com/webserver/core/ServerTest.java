package com.webserver.core;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    
    @Test
    void testServerStartsAndAcceptsConnections() {
        // Start server in separate thread
        Server server = new Server(8081, 2);
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        // Give server time to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Try to connect
        try {
            Socket socket = new Socket("localhost", 8081);
            assertTrue(socket.isConnected());
            socket.close();
        } catch (IOException e) {
            fail("Could not connect to server: " + e.getMessage());
        } finally {
            server.stop();
        }
    }
} 