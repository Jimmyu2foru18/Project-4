package com.webserver.core;

import com.webserver.util.Logger;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final Logger logger = new Logger(Server.class);
    private final int port;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public Server(int port, int threadPoolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.info("Server started on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            logger.error("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleClient(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            
            String request = in.readLine();
            if (request != null) {
                String response = processRequest(request);
                out.println(response);
            }
        } catch (IOException e) {
            logger.error("Error handling client: " + e.getMessage());
        }
    }

    private String processRequest(String request) {
        // Basic request processing
        return "HTTP/1.0 200 OK\r\n\r\nHello, World!";
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error stopping server: " + e.getMessage());
        }
    }
} 