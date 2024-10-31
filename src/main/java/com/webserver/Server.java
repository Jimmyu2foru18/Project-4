package com.webserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.Map;

public class Server {
    private final int port;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private final Map<Socket, Long> persistentConnections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long KEEP_ALIVE_TIMEOUT = 30000; // 30 seconds

    public Server(int port, int threadPoolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        scheduler.scheduleAtFixedRate(this::cleanupConnections, 10, 10, TimeUnit.SECONDS);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);

            while (running) {
                Socket client = serverSocket.accept();
                threadPool.execute(() -> handleClient(client));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            boolean keepAlive = false;
            
            while ((line = in.readLine()) != null) {
                requestBuilder.append(line).append("\r\n");
                if (line.toLowerCase().contains("connection: keep-alive")) {
                    keepAlive = true;
                }
                if (line.isEmpty()) break;
            }

            String response = processRequest(requestBuilder.toString());
            out.println(response);

            if (keepAlive) {
                persistentConnections.put(socket, System.currentTimeMillis());
                socket.setSoTimeout((int) KEEP_ALIVE_TIMEOUT);
            } else {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private void cleanupConnections() {
        long now = System.currentTimeMillis();
        persistentConnections.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > KEEP_ALIVE_TIMEOUT) {
                try {
                    entry.getKey().close();
                    return true;
                } catch (IOException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
            return false;
        });
    }

    private String processRequest(String request) {
        if (!request.startsWith("GET")) {
            return "HTTP/1.0 400 Bad Request\r\n\r\nOnly GET method is supported";
        }

        String[] parts = request.split(" ");
        if (parts.length < 2) {
            return "HTTP/1.0 400 Bad Request\r\n\r\nMalformed request";
        }

        String path = parts[1];
        if (path.equals("/")) {
            path = "/index.html";
        }

        try {
            Path filePath = Paths.get("public", path.substring(1));
            if (!filePath.normalize().startsWith("public")) {
                return "HTTP/1.0 403 Forbidden\r\n\r\nAccess denied";
            }

            if (Files.exists(filePath)) {
                byte[] content = Files.readAllBytes(filePath);
                String contentType = getContentType(path);
                return String.format("HTTP/1.0 200 OK\r\nContent-Type: %s\r\nContent-Length: %d\r\n\r\n",
                    contentType, content.length) + new String(content);
            } else {
                return "HTTP/1.0 404 Not Found\r\n\r\nFile not found: " + path;
            }
        } catch (IOException e) {
            return "HTTP/1.0 500 Internal Server Error\r\n\r\n" + e.getMessage();
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".txt")) return "text/plain";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new Server(port, 10).start();
    }
} 