package com.webserver.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private final Class<?> sourceClass;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logger(Class<?> sourceClass) {
        this.sourceClass = sourceClass;
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void debug(String message) {
        log("DEBUG", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }

    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] %s - %s: %s%n", 
            timestamp, level, sourceClass.getSimpleName(), message);
    }
} 