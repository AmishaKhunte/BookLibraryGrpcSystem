package com.example.libraryservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BookNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Book not found", ex.getMessage()));
    }

    @ExceptionHandler(InvalidBookIdException.class)
    public ResponseEntity<ApiError> handleInvalidId(InvalidBookIdException ex) {
        return ResponseEntity.badRequest().body(new ApiError("Invalid book ID", ex.getMessage()));
    }

    @ExceptionHandler(BookServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(BookServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("Book service unavailable", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Internal server error", "An unexpected error occurred."));
    }
}
