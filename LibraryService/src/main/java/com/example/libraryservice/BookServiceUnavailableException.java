package com.example.libraryservice;

public class BookServiceUnavailableException extends RuntimeException {
    public BookServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
