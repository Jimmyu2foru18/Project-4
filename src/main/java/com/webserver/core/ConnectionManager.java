package com.webserver.core;

import com.webserver.util.Constants;
import com.webserver.util.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {
    private static final Logger logger = new Logger(ConnectionManager.class);
    private final Map<Socket, Long> keepAliveConnections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long KEEP_ALIVE_TIMEOUT = Constants.KEEP_ALIVE_TIMEOUT; // 30 seconds
    private static final long CLEANUP_INTERVAL = Constants.CLEANUP_INTERVAL; // 10 seconds

    public ConnectionManager() {
        startCleanupTask();
    }

    public void addConnection(Socket socket, boolean keepAlive) {
        if (keepAlive) {
            keepAliveConnections.put(socket, System.currentTimeMillis());
            logger.info("Added keep-alive connection: " + socket.getRemoteSocketAddress());
        }
    }

    public boolean isConnectionKeptAlive(Socket socket) {
        return keepAliveConnections.containsKey(socket);
    }

    public void updateConnectionTime(Socket socket) {
        if (isConnectionKeptAlive(socket)) {
            keepAliveConnections.put(socket, System.currentTimeMillis());
        }
    }

    public void removeConnection(Socket socket) {
        keepAliveConnections.remove(socket);
        try {
            if (!socket.isClosed()) {
                socket.close();
                logger.info("Closed keep-alive connection: " + socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            logger.error("Error closing socket: " + e.getMessage());
        }
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            keepAliveConnections.entrySet().removeIf(entry -> {
                if (currentTime - entry.getValue() > KEEP_ALIVE_TIMEOUT) {
                    try {
                        Socket socket = entry.getKey();
                        if (!socket.isClosed()) {
                            socket.close();
                        }
                        logger.info("Closed inactive keep-alive connection: " + socket.getRemoteSocketAddress());
                        return true;
                    } catch (IOException e) {
                        logger.error("Error closing inactive connection: " + e.getMessage());
                        return true;
                    }
                }
                return false;
            });
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close all remaining connections
        keepAliveConnections.keySet().forEach(this::removeConnection);
    }
} 