package com.webserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties props = new Properties();
    
    static {
        try (InputStream input = Config.class.getClassLoader()
                .getResourceAsStream("server.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load server properties", e);
        }
    }
    
    public static int getAcceptTimeout() {
        return Integer.parseInt(props.getProperty("server.accept.timeout", "1000"));
    }
    
    // ... other getters
} 