package com.webserver.load;

import java.util.List;
import java.util.logging.Logger;

public class LoadTester {
    private static final Logger logger = Logger.getLogger(LoadTester.class.getName());

    public void printResults(List<Long> responseTimes) {
        double avg = responseTimes.stream()
            .mapToLong(Long::valueOf)
            .average()
            .orElse(0);
        
        logger.info("=== Load Test Results ===");
        logger.info("Average response time: " + avg + "ms");
        // ... rest of the method
    }
} 