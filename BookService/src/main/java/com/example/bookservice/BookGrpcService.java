package com.example.bookservice;

import com.example.booklibrary.grpc.Book;
import com.example.booklibrary.grpc.BookServiceGrpc;
import com.example.booklibrary.grpc.GetBookRequest;
import com.example.booklibrary.grpc.GetBookResponse;
import com.example.booklibrary.grpc.ListBooksRequest;
import com.example.booklibrary.grpc.ListBooksResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {
    private final Map<Integer, Book> books = new LinkedHashMap<>();

    public BookGrpcService() {
        addBook(1, "Clean Code", "Robert C. Martin", "9780132350884", true);
        addBook(2, "Effective Java", "Joshua Bloch", "9780134685991", true);
        addBook(3, "Head First Java", "Kathy Sierra and Bert Bates", "9780596009205", false);
        addBook(4, "Spring in Action", "Craig Walls", "9781617297571", true);
        addBook(5, "Java: The Complete Reference", "Herbert Schildt", "9781260440232", true);
    }

    private void addBook(int id, String title, String author, String isbn, boolean available) {
        books.put(id, Book.newBuilder()
                .setId(id)
                .setTitle(title)
                .setAuthor(author)
                .setIsbn(isbn)
                .setAvailable(available)
                .build());
    }

    @Override
    public void getBook(GetBookRequest request, StreamObserver<GetBookResponse> responseObserver) {
        Book book = books.get(request.getId());
        GetBookResponse.Builder response = GetBookResponse.newBuilder();
        if (book == null) {
            response.setFound(false)
                    .setMessage("No book found with id " + request.getId());
        } else {
            response.setFound(true)
                    .setBook(book)
                    .setMessage("Book found");
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listBooks(ListBooksRequest request, StreamObserver<ListBooksResponse> responseObserver) {
        ListBooksResponse response = ListBooksResponse.newBuilder()
                .addAllBooks(books.values())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
