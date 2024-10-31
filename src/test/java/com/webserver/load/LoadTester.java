package com.webserver.load;

import com.webserver.core.Client;
import com.webserver.util.Logger;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTester {
    private static final Logger logger = new Logger(LoadTester.class);
    private final String host;
    private final int port;
    private final int numThreads;
    private final int numRequests;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();

    public LoadTester(String host, int port, int numThreads, int numRequests) {
        this.host = host;
        this.port = port;
        this.numThreads = numThreads;
        this.numRequests = numRequests;
    }

    public void runTest() {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numRequests);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                try {
                    Client client = new Client(host, port);
                    long requestStart = System.currentTimeMillis();
                    String response = client.sendGetRequest("/test.html");
                    long requestTime = System.currentTimeMillis() - requestStart;
                    
                    responseTimes.offer(requestTime);
                    
                    if (response.contains("200 OK")) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.error("Request failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        printResults(System.currentTimeMillis() - startTime);
    }

    private void printResults(long totalTime) {
        double avg = responseTimes.stream()
            .mapToLong(Long::valueOf)
            .average()
            .orElse(0);
        
        logger.info("=== Load Test Results ===");
        logger.info("Total Requests: " + numRequests);
        logger.info("Successful Requests: " + successCount.get());
        logger.info("Failed Requests: " + failureCount.get());
        logger.info("Average Response Time: " + avg + "ms");
        logger.info("Total Test Time: " + totalTime + "ms");
        logger.info("Requests/Second: " + (numRequests * 1000.0 / totalTime));
    }
} 