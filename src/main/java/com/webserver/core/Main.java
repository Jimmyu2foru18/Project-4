package com.webserver.core;

public class Main {
    public static void main(String[] args) {
        int port = Constants.DEFAULT_PORT;
        int threadPoolSize = Constants.DEFAULT_THREAD_POOL_SIZE;

        Server server = new Server(port, threadPoolSize);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
} 