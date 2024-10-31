package com.webserver.http;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private String version;
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers;
    private byte[] body;

    public HttpResponse() {
        this.version = "HTTP/1.0";
        this.headers = new HashMap<>();
    }

    public void setStatus(HttpStatus status) {
        this.statusCode = status.getCode();
        this.statusMessage = status.getMessage();
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(byte[] body) {
        this.body = body;
        addHeader("Content-Length", String.valueOf(body.length));
    }

    public byte[] getBytes() {
        StringBuilder response = new StringBuilder();
        
        // Status line
        response.append(version).append(" ")
               .append(statusCode).append(" ")
               .append(statusMessage).append("\r\n");

        // Headers
        headers.forEach((key, value) -> 
            response.append(key).append(": ").append(value).append("\r\n"));

        // Empty line separating headers from body
        response.append("\r\n");

        // Convert headers to bytes and combine with body
        byte[] headerBytes = response.toString().getBytes();
        if (body == null) {
            return headerBytes;
        }

        // Combine headers and body
        byte[] fullResponse = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, fullResponse, 0, headerBytes.length);
        System.arraycopy(body, 0, fullResponse, headerBytes.length, body.length);

        return fullResponse;
    }
} 