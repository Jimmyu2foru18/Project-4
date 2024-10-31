package com.webserver.handler;

import com.webserver.core.ConnectionManager;
import com.webserver.http.HttpRequest;
import com.webserver.http.HttpResponse;
import com.webserver.http.HttpStatus;
import com.webserver.util.Constants;
import com.webserver.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RequestHandler implements Runnable {
    private static final Logger logger = new Logger(RequestHandler.class);
    private final Socket clientSocket;
    private final ConnectionManager connectionManager;
    private static final String PUBLIC_DIR = Constants.PUBLIC_DIR;
    private static final int SOCKET_TIMEOUT = Constants.SOCKET_TIMEOUT;
    private static final int MAX_PIPELINE_REQUESTS = 10;

    public RequestHandler(Socket clientSocket, ConnectionManager connectionManager) {
        this.clientSocket = clientSocket;
        this.connectionManager = connectionManager;
        try {
            this.clientSocket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (IOException e) {
            logger.error("Failed to set socket timeout: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = new BufferedOutputStream(clientSocket.getOutputStream())) {
            
            int pipelinedRequests = 0;
            boolean keepAlive = true;

            while (keepAlive && pipelinedRequests < MAX_PIPELINE_REQUESTS) {
                // Try to read the next request
                HttpRequest request = readRequest(in);
                if (request == null) {
                    break; // No more requests
                }

                logger.info("Received request: " + request);

                // Check if client wants to keep connection alive
                keepAlive = isKeepAliveRequest(request);

                // Handle request and get response
                HttpResponse response = handleRequest(request);

                // Add keep-alive header if supported
                if (keepAlive) {
                    response.addHeader("Connection", "keep-alive");
                    response.addHeader("Keep-Alive", "timeout=30, max=" + (MAX_PIPELINE_REQUESTS - pipelinedRequests));
                } else {
                    response.addHeader("Connection", "close");
                }

                // Send response
                out.write(response.getBytes());
                out.flush();

                pipelinedRequests++;

                // Update connection time if keep-alive
                if (keepAlive) {
                    connectionManager.updateConnectionTime(clientSocket);
                }
            }
            
        } catch (IOException e) {
            logger.error("Error handling request: " + e.getMessage());
            sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (!connectionManager.isConnectionKeptAlive(clientSocket)) {
                closeQuietly(clientSocket);
            }
        }
    }

    private HttpRequest readRequest(BufferedReader in) throws IOException {
        try {
            return HttpRequest.parse(in);
        } catch (IOException e) {
            if (e.getMessage().contains("Empty request line")) {
                return null; // Normal end of stream
            }
            throw e; // Re-throw other IOExceptions
        }
    }

    private boolean isKeepAliveRequest(HttpRequest request) {
        String connection = request.getHeader("connection");
        if (connection != null) {
            return "keep-alive".equalsIgnoreCase(connection);
        }
        // HTTP/1.1 defaults to keep-alive, HTTP/1.0 defaults to close
        return request.getVersion().equals("HTTP/1.1");
    }

    private HttpResponse handleRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        try {
            // Validate request method
            if (!"GET".equals(request.getMethod())) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Only GET method is supported");
            }

            // Validate and normalize path
            String normalizedPath = normalizePath(request.getPath());
            if (normalizedPath == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid path");
            }

            // Resolve and validate file path
            Path filePath = resolveFilePath(normalizedPath);
            if (!isValidPath(filePath)) {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Resource not found");
            }

            // Serve file
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                response.setStatus(HttpStatus.OK);
                response.addHeader("Content-Type", getContentType(filePath.toString()));
                response.setBody(Files.readAllBytes(filePath));
            } else {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Resource not found");
            }

        } catch (IOException e) {
            logger.error("Error serving file: " + e.getMessage());
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");
        }

        return response;
    }

    private String normalizePath(String path) {
        if (path == null) return null;
        path = path.replaceAll("/+", "/");
        if (path.contains("..")) return null; // Prevent directory traversal
        return path;
    }

    private Path resolveFilePath(String requestPath) {
        if ("/".equals(requestPath)) {
            requestPath = Constants.DEFAULT_FILE;
        }
        return Paths.get(PUBLIC_DIR, requestPath);
    }

    private boolean isValidPath(Path filePath) {
        try {
            Path publicDirPath = Paths.get(PUBLIC_DIR).toRealPath();
            Path normalizedFilePath = filePath.toRealPath();
            return normalizedFilePath.startsWith(publicDirPath);
        } catch (IOException e) {
            return false;
        }
    }

    private String getContentType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    private HttpResponse createErrorResponse(HttpStatus status, String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(status);
        response.addHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody(message.getBytes());
        return response;
    }

    private void sendErrorResponse(HttpStatus status) {
        try {
            HttpResponse response = createErrorResponse(status, status.getMessage());
            clientSocket.getOutputStream().write(response.getBytes());
        } catch (IOException ignored) {
            // Already in error handling, nothing more to do
        }
    }

    private void closeQuietly(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing socket: " + e.getMessage());
        }
    }
} 