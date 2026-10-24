# Testing with Insomnia

This project is a Spring Boot gRPC system with two services:

- BookService: exposes the gRPC server
- LibraryService: acts as a gRPC client and points to `localhost:9090`

## 1. Start the gRPC server

Open a terminal in the `BookService` folder and run:

```bash
mvn spring-boot:run
```

The server should start and listen on:

```text
localhost:9090
```

## 2. Open Insomnia

Create a new request and choose the `gRPC` type.

Set the server URL to:

```text
localhost:9090
```

## 3. Import the proto file

Use the proto file from:

```text
BookService/src/main/proto/book.proto
```

This defines the service:

```proto
service BookService {
  rpc GetBook(GetBookRequest) returns (GetBookResponse);
  rpc ListBooks(ListBooksRequest) returns (ListBooksResponse);
}
```

In Insomnia, the generated service should appear as:

```text
booklibrary.BookService
```

## 4. Test the RPC methods

### GetBook

Method:

```text
booklibrary.BookService/GetBook
```

Request body:

```json
{
  "id": 1
}
```

Example response:

```json
{
  "book": {
    "id": 1,
    "title": "Sample Book",
    "author": "Jane Doe",
    "isbn": "123456789",
    "available": true
  },
  "found": true,
  "message": "Book found"
}
```

### ListBooks

Method:

```text
booklibrary.BookService/ListBooks
```

Request body:

```json
{}
```

Example response:

```json
{
  "books": [
    {
      "id": 1,
      "title": "Sample Book",
      "author": "Jane Doe",
      "isbn": "123456789",
      "available": true
    }
  ]
}
```

## 5. Client-side config

The gRPC client is configured in:

```text
LibraryService/src/main/resources/application.properties
```

```properties
server.port=8080
spring.application.name=library-service
grpc.client.target=localhost:9090
```

This means the client expects BookService to be running locally on port `9090`.

## Notes

- The BookService RPC definitions are in `book.proto`.
- The BookService must be started before using Insomnia to call the gRPC methods.
- If the server is not reachable, Insomnia will return a connection error.
