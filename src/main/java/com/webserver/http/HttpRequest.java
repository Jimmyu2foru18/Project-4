package com.webserver.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;

    private HttpRequest() {
        this.headers = new HashMap<>();
    }

    public static HttpRequest parse(BufferedReader reader) throws IOException {
        HttpRequest request = new HttpRequest();
        
        // Parse request line
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.trim().isEmpty()) {
            throw new IOException("Empty request line");
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }

        request.method = parts[0].toUpperCase();
        request.path = parts[1];
        request.version = parts[2].toUpperCase();

        // Validate HTTP version
        if (!request.version.startsWith("HTTP/")) {
            throw new IOException("Invalid HTTP version: " + request.version);
        }

        // Parse headers
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int separatorIndex = headerLine.indexOf(':');
            if (separatorIndex > 0) {
                String key = headerLine.substring(0, separatorIndex).trim();
                String value = headerLine.substring(separatorIndex + 1).trim();
                request.headers.put(key.toLowerCase(), value);
            }
        }

        // Parse body if Content-Length is present
        String contentLengthStr = request.headers.get("content-length");
        if (contentLengthStr != null) {
            int contentLength = Integer.parseInt(contentLengthStr);
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int bytesRead = reader.read(bodyChars, 0, contentLength);
                if (bytesRead > 0) {
                    request.body = new String(bodyChars, 0, bytesRead);
                }
            }
        }

        return request;
    }

    // Getters
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getVersion() { return version; }
    public Map<String, String> getHeaders() { return new HashMap<>(headers); }
    public String getBody() { return body; }

    // Utility methods
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public boolean hasHeader(String name) {
        return headers.containsKey(name.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", method, path, version);
    }
} 