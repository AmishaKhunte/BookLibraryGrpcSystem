package com.example.libraryservice;

public record BookDto(int id, String title, String author, String isbn, boolean available) {
}
