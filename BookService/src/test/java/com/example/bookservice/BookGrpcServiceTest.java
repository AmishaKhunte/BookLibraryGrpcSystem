package com.example.bookservice;

import com.example.booklibrary.grpc.GetBookRequest;
import com.example.booklibrary.grpc.GetBookResponse;
import com.example.booklibrary.grpc.ListBooksRequest;
import com.example.booklibrary.grpc.ListBooksResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BookGrpcServiceTest {
    private final BookGrpcService service = new BookGrpcService();

    @Test
    void returnsBookWhenIdExists() throws Exception {
        AtomicReference<GetBookResponse> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.getBook(GetBookRequest.newBuilder().setId(1).build(), new StreamObserver<>() {
            public void onNext(GetBookResponse value) { result.set(value); }
            public void onError(Throwable t) { fail(t); latch.countDown(); }
            public void onCompleted() { latch.countDown(); }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(result.get().getFound());
        assertEquals("Clean Code", result.get().getBook().getTitle());
    }

    @Test
    void reportsMissingBook() throws Exception {
        AtomicReference<GetBookResponse> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.getBook(GetBookRequest.newBuilder().setId(999).build(), new StreamObserver<>() {
            public void onNext(GetBookResponse value) { result.set(value); }
            public void onError(Throwable t) { fail(t); latch.countDown(); }
            public void onCompleted() { latch.countDown(); }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(result.get().getFound());
    }

    @Test
    void listsFiveBooks() throws Exception {
        AtomicReference<ListBooksResponse> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.listBooks(ListBooksRequest.getDefaultInstance(), new StreamObserver<>() {
            public void onNext(ListBooksResponse value) { result.set(value); }
            public void onError(Throwable t) { fail(t); latch.countDown(); }
            public void onCompleted() { latch.countDown(); }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(5, result.get().getBooksCount());
    }
}
