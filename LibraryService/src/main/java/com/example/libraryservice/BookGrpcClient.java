package com.example.libraryservice;

import com.example.booklibrary.grpc.Book;
import com.example.booklibrary.grpc.GetBookRequest;
import com.example.booklibrary.grpc.GetBookResponse;
import com.example.booklibrary.grpc.ListBooksRequest;
import com.example.booklibrary.grpc.ListBooksResponse;
import com.example.booklibrary.grpc.BookServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookGrpcClient {
    private final BookServiceGrpc.BookServiceBlockingStub stub;

    public BookGrpcClient(BookServiceGrpc.BookServiceBlockingStub stub) {
        this.stub = stub;
    }

    public BookDto getBook(int id) {
        try {
            GetBookResponse response = stub.withDeadlineAfter(2, java.util.concurrent.TimeUnit.SECONDS)
                    .getBook(GetBookRequest.newBuilder().setId(id).build());
            if (!response.getFound()) {
                throw new BookNotFoundException(response.getMessage());
            }
            return toDto(response.getBook());
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.UNAVAILABLE
                    || ex.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw unavailable(ex);
            }
            throw ex;
        }
    }

    public List<BookDto> listBooks() {
        try {
            ListBooksResponse response = stub.withDeadlineAfter(2, java.util.concurrent.TimeUnit.SECONDS)
                    .listBooks(ListBooksRequest.getDefaultInstance());
            return response.getBooksList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.UNAVAILABLE
                    || ex.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw unavailable(ex);
            }
            throw ex;
        }
    }

    private BookDto toDto(Book book) {
        return new BookDto(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getAvailable());
    }

    private BookServiceUnavailableException unavailable(StatusRuntimeException ex) {
        return new BookServiceUnavailableException(
                "The Book Service could not be reached. Make sure it is running on localhost:9090.", ex);
    }
}
