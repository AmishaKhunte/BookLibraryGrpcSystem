package com.example.libraryservice;

public class InvalidBookIdException extends RuntimeException {
    public InvalidBookIdException(String message) {
        super(message);
    }
}
