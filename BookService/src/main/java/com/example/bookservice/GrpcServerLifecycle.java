package com.example.bookservice;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServerLifecycle implements SmartLifecycle {
    private final BookGrpcService bookGrpcService;
    private volatile Server server;
    private volatile boolean running;

    public GrpcServerLifecycle(BookGrpcService bookGrpcService) {
        this.bookGrpcService = bookGrpcService;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        try {
            server = ServerBuilder.forPort(9090)
                    .addService(bookGrpcService)
                    .build()
                    .start();
            running = true;
            System.out.println("Book Service gRPC server started on port 9090");
        } catch (IOException ex) {
            throw new IllegalStateException("Could not start gRPC server on port 9090", ex);
        }
    }

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException ex) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
