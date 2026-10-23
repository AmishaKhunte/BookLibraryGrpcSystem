package com.example.libraryservice;

import com.example.booklibrary.grpc.BookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfiguration {
    @Bean(destroyMethod = "shutdown")
    public ManagedChannel bookServiceChannel(
            @Value("${grpc.client.target:localhost:9090}") String target) {
        return ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
    }

    @Bean
    public BookServiceGrpc.BookServiceBlockingStub bookServiceBlockingStub(ManagedChannel channel) {
        return BookServiceGrpc.newBlockingStub(channel);
    }
}
