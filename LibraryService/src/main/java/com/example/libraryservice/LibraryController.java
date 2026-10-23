package com.example.libraryservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library/books")
public class LibraryController {
    private final BookGrpcClient bookGrpcClient;

    public LibraryController(BookGrpcClient bookGrpcClient) {
        this.bookGrpcClient = bookGrpcClient;
    }

    @GetMapping
    public List<BookDto> listBooks() {
        return bookGrpcClient.listBooks();
    }

    @GetMapping("/{id}")
    public BookDto getBook(@PathVariable int id) {
        validateId(id);
        return bookGrpcClient.getBook(id);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityDto getAvailability(@PathVariable int id) {
        validateId(id);
        BookDto book = bookGrpcClient.getBook(id);
        return new AvailabilityDto(book.id(), book.title(), book.available(),
                book.available() ? "Book is available" : "Book is currently unavailable");
    }

    private void validateId(int id) {
        if (id <= 0) {
            throw new InvalidBookIdException("Book ID must be a positive integer");
        }
    }
}
