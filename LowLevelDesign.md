# Low Level Design (LLD) - BookLibraryGrpcSystem

## 1. Purpose

This document explains the implementation-level design of the BookLibraryGrpcSystem in a simple and detailed manner.

The system has two Spring Boot services:

- BookService: a gRPC server that owns the book catalog and exposes business operations through proto-generated methods.
- LibraryService: a Spring Boot REST service that uses a gRPC client stub to call BookService and then exposes REST endpoints to external clients.

This is not just a conceptual design. It reflects the actual structure of the repository and the code patterns used in the implementation.

---

## 2. Scope of the System

The scope of the current system is deliberately small and focused:

- list all books
- get one book by id
- check if a book is available
- handle invalid ID and service unavailable cases

The project demonstrates:

- Spring Boot microservice setup
- gRPC service contract definition with .proto files
- server startup and lifecycle management
- gRPC client configuration
- REST controller and DTO mapping
- exception handling strategies

---

## 3. Repository Structure

The repository contains two main services.

### 3.1 BookService

Folder:

- BookService

Key contents:

- pom.xml
- src/main/java/com/example/bookservice/
- src/main/proto/book.proto
- src/main/resources/

Important classes:

- BookServiceApplication
- GrpcServerLifecycle
- BookGrpcService

### 3.2 LibraryService

Folder:

- LibraryService

Key contents:

- pom.xml
- src/main/java/com/example/libraryservice/
- src/main/resources/application.properties

Important classes:

- LibraryServiceApplication
- LibraryController
- BookGrpcClient
- GrpcClientConfiguration
- BookDto
- AvailabilityDto
- ApiError
- ApiExceptionHandler
- BookNotFoundException
- InvalidBookIdException
- BookServiceUnavailableException

---

## 4. Business Flow

The system works like this:

1. A client sends an HTTP request to LibraryService.
2. LibraryController receives that HTTP request.
3. LibraryController calls BookGrpcClient.
4. BookGrpcClient uses a gRPC blocking stub to call BookService.
5. BookService processes the request and returns a gRPC response.
6. LibraryService transforms the result into a DTO and returns it as JSON.

This means the external API is REST, but the internal service-to-service communication is gRPC.

---

## 5. Detailed Design of BookService

## 5.1 Startup Class

File:

- BookService/src/main/java/com/example/bookservice/BookServiceApplication.java

Code behavior:

- This is the main Spring Boot entry point.
- It runs SpringApplication.run(BookServiceApplication.class, args).

Purpose:

- Start the Spring container
- Initialize application configuration
- Trigger bean creation for services and lifecycle components

Example flow:

```java
@SpringBootApplication
public class BookServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookServiceApplication.class, args);
    }
}
```

---

## 5.2 gRPC Lifecycle Component

File:

- BookService/src/main/java/com/example/bookservice/GrpcServerLifecycle.java

This class implements Spring's SmartLifecycle.

Why it matters:

- It allows the gRPC server to start automatically when the application starts.
- It manages the lifecycle of the gRPC server in a Spring-aware manner.

Fields:

- BookGrpcService bookGrpcService
- Server server
- boolean running

Constructor:

```java
public GrpcServerLifecycle(BookGrpcService bookGrpcService) {
    this.bookGrpcService = bookGrpcService;
}
```

Core method: start()

```java
@Override
public synchronized void start() {
    if (running) {
        return;
    }
    try {
        server = ServerBuilder.forPort(9090)
                .addService(bookGrpcService)
                .build()
                .start();
        running = true;
        System.out.println("Book Service gRPC server started on port 9090");
    } catch (IOException ex) {
        throw new IllegalStateException("Could not start gRPC server on port 9090", ex);
    }
}
```

Important details:

- Port 9090 is hardcoded.
- A grpc server is created using io.grpc.ServerBuilder.
- The BookGrpcService implementation is attached using .addService(bookGrpcService).
- The server starts automatically when the Spring application is running.

Stop logic:

```java
@Override
public synchronized void stop() {
    if (server != null) {
        server.shutdown();
        try {
            if (!server.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException ex) {
            server.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    running = false;
}
```

This ensures safe shutdown and avoids hanging threads.

Lifecycle contract overrides:

- stop(Runnable callback)
- isRunning()
- isAutoStartup()
- getPhase()

These tell Spring the component should run automatically and should be shut down in an orderly way.

---

## 5.3 gRPC Service Implementation

File:

- BookService/src/main/java/com/example/bookservice/BookGrpcService.java

Class details:

```java
@Service
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {
    private final Map<Integer, Book> books = new LinkedHashMap<>();
```

This tells Spring:

- this class is a service bean
- it implements the generated gRPC service framework created from the proto file

Why extends BookServiceGrpc.BookServiceImplBase:

- The proto compiler generates a base class for the service.
- We override methods to implement the logic.

The service has an in-memory data set:

```java
public BookGrpcService() {
    addBook(1, "Clean Code", "Robert C. Martin", "9780132350884", true);
    addBook(2, "Effective Java", "Joshua Bloch", "9780134685991", true);
    addBook(3, "Head First Java", "Kathy Sierra and Bert Bates", "9780596009205", false);
    addBook(4, "Spring in Action", "Craig Walls", "9781617297571", true);
    addBook(5, "Java: The Complete Reference", "Herbert Schildt", "9781260440232", true);
}
```

The books are stored in a LinkedHashMap so their insertion order is preserved.

Helper method:

```java
private void addBook(int id, String title, String author, String isbn, boolean available) {
    books.put(id, Book.newBuilder()
            .setId(id)
            .setTitle(title)
            .setAuthor(author)
            .setIsbn(isbn)
            .setAvailable(available)
            .build());
}
```

This converts plain Java values into generated protobuf Book message objects.

---

## 5.4 GetBook Method

The service implements:

```java
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
```

What happens here:

- request.getId() reads the incoming id
- books.get(id) checks the in-memory storage
- if not found:
  - found = false
  - message = "No book found with id X"
- if found:
  - found = true
  - book field is populated
  - message = "Book found"
- responseObserver.onNext() sends the response
- responseObserver.onCompleted() ends the call

This is a unary RPC pattern: one request, one response.

---

## 5.5 ListBooks Method

```java
@Override
public void listBooks(ListBooksRequest request, StreamObserver<ListBooksResponse> responseObserver) {
    ListBooksResponse response = ListBooksResponse.newBuilder()
            .addAllBooks(books.values())
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

What happens here:

- extracts all book values from the map
- builds ListBooksResponse
- adds the list as repeated Book in protocol buffers
- sends it back over gRPC

This is also a unary method returning a collection of messages.

---

## 6. Detailed Design of the gRPC Contract

File:

- BookService/src/main/proto/book.proto

This is the contract definition used by both services.

### 6.1 File Structure

```proto
syntax = "proto3";

package booklibrary;

option java_package = "com.example.booklibrary.grpc";
option java_multiple_files = true;
option java_outer_classname = "BookLibraryProto";
```

Why this matters:

- syntax = "proto3" tells Protobuf to use version 3 syntax.
- package booklibrary defines the namespace.
- java_package defines the Java package for generated classes.
- java_multiple_files = true generates separate classes for each message/service instead of one huge file.
- java_outer_classname gives a wrapper class name.

---

## 6.2 Service Definition

```proto
service BookService {
  rpc GetBook(GetBookRequest) returns (GetBookResponse);
  rpc ListBooks(ListBooksRequest) returns (ListBooksResponse);
}
```

This means:

- service name: BookService
- RPC method 1: GetBook
- RPC method 2: ListBooks

Generated Java stubs will include classes such as:

- BookServiceGrpc
- BookServiceGrpc.BookServiceBlockingStub
- BookServiceGrpc.BookServiceImplBase

This is the core API contract used by both the server and the client.

---

## 6.3 Message Definitions

### Book

```proto
message Book {
  int32 id = 1;
  string title = 2;
  string author = 3;
  string isbn = 4;
  bool available = 5;
}
```

This describes a single book and its fields.

Field numbers:

- id = 1
- title = 2
- author = 3
- isbn = 4
- available = 5

These field numbers are permanent for compatibility. They are used in Protobuf encoding.

### GetBookRequest

```proto
message GetBookRequest {
  int32 id = 1;
}
```

This is the request sent to find a book by ID.

### GetBookResponse

```proto
message GetBookResponse {
  Book book = 1;
  bool found = 2;
  string message = 3;
}
```

This response returns:

- the Book object itself
- whether the book was found
- a human-readable message

### ListBooksRequest

```proto
message ListBooksRequest {}
```

An empty request is enough to ask for all books.

### ListBooksResponse

```proto
message ListBooksResponse {
  repeated Book books = 1;
}
```

This is a repeated list of Book objects.

---

## 7. Detailed Design of LibraryService

## 7.1 Spring Boot Application Entry

File:

- LibraryService/src/main/java/com/example/libraryservice/LibraryServiceApplication.java

This is the main entry class for the REST service.

```java
@SpringBootApplication
public class LibraryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryServiceApplication.class, args);
    }
}
```

This starts the REST API and wires Spring beans.

---

## 7.2 HTTP Controller

File:

- LibraryService/src/main/java/com/example/libraryservice/LibraryController.java

This class is the REST layer.

It uses:

```java
@RestController
@RequestMapping("/api/library/books")
public class LibraryController {
    private final BookGrpcClient bookGrpcClient;
```

Base path:

```text
/api/library/books
```

### Method 1: listBooks

```java
@GetMapping
public List<BookDto> listBooks() {
    return bookGrpcClient.listBooks();
}
```

This endpoint calls the gRPC client and returns a list of DTOs.

### Method 2: getBook

```java
@GetMapping("/{id}")
public BookDto getBook(@PathVariable int id) {
    validateId(id);
    return bookGrpcClient.getBook(id);
}
```

Workflow:

- gets `id` from URL
- validates positive integer
- passes to gRPC client
- returns BookDto JSON

### Method 3: getAvailability

```java
@GetMapping("/{id}/availability")
public AvailabilityDto getAvailability(@PathVariable int id) {
    validateId(id);
    BookDto book = bookGrpcClient.getBook(id);
    return new AvailabilityDto(book.id(), book.title(), book.available(),
            book.available() ? "Book is available" : "Book is currently unavailable");
}
```

This endpoint:

- validates id
- makes a gRPC call to fetch the book
- builds a response object with availability info

### Validation method

```java
private void validateId(int id) {
    if (id <= 0) {
        throw new InvalidBookIdException("Book ID must be a positive integer");
    }
}
```

If ID is invalid, business exception is thrown.

---

## 7.3 BookGrpcClient

File:

- LibraryService/src/main/java/com/example/libraryservice/BookGrpcClient.java

This is the service that calls BookService through gRPC.

Class definition:

```java
@Service
public class BookGrpcClient {
    private final BookServiceGrpc.BookServiceBlockingStub stub;
```

Important design fact:

- It uses a blocking stub instead of async stub.
- That makes the client easier to understand and is suitable for simple request-response flows.

### Constructor

```java
public BookGrpcClient(BookServiceGrpc.BookServiceBlockingStub stub) {
    this.stub = stub;
}
```

The stub is created by Spring via GrpcClientConfiguration.

---

## 7.4 getBook implementation

```java
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
```

Detailed behavior:

- creates request with id
- calls `stub.getBook(...)`
- sets a 2-second deadline
- if response says found=false, throws BookNotFoundException
- if found=true, converts protobuf Book to BookDto
- if gRPC connection fails or timeout occurs, converts it to BookServiceUnavailableException

Why deadline is important:

- prevents the client from hanging forever
- provides resilience when the book service is down

---

## 7.5 listBooks implementation

```java
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
```

This does:

- calls listBooks with empty request
- gets all books
- maps each protobuf Book to BookDto using `toDto`
- converts stream to List

---

## 7.6 Conversion helper

```java
private BookDto toDto(Book book) {
    return new BookDto(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getAvailable());
}
```

This method transforms the gRPC-generated Book object into the REST-facing BookDto record.

This is important because the REST layer should not expose protobuf-generated classes directly.

---

## 7.7 Unavailable exception helper

```java
private BookServiceUnavailableException unavailable(StatusRuntimeException ex) {
    return new BookServiceUnavailableException(
            "The Book Service could not be reached. Make sure it is running on localhost:9090.", ex);
}
```

This ensures a friendly message is returned to the client if the gRPC server is not up.

---

## 7.8 GrpcClientConfiguration

File:

- LibraryService/src/main/java/com/example/libraryservice/GrpcClientConfiguration.java

This class configures the gRPC client connection.

```java
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
```

Detailed behavior:

- A `ManagedChannel` is created for the target address defined in `application.properties`.
- The code uses `forTarget(target)` and `usePlaintext()`.
- This is acceptable for local demo usage, but not recommended for production without TLS.
- The blocking stub is created from the channel.

This is the exact place where the client knows the BookService location.

---

## 7.9 Configuration Properties

File:

- LibraryService/src/main/resources/application.properties

Contents:

```properties
server.port=8080
spring.application.name=library-service
grpc.client.target=localhost:9090
```

Meaning:

- REST server runs on port 8080
- app name is library-service
- gRPC book service expected at localhost:9090

So the API façade runs on port 8080, while the actual backend service runs on port 9090.

---

## 8. REST DTOs

## 8.1 BookDto

File:

- LibraryService/src/main/java/com/example/libraryservice/BookDto.java

```java
public record BookDto(int id, String title, String author, String isbn, boolean available) {
}
```

This is a plain DTO used to return JSON to HTTP clients.

It contains:

- id
- title
- author
- isbn
- available

This is simpler and more friendly than returning protobuf-generated objects.

---

## 8.2 AvailabilityDto

```java
public record AvailabilityDto(int id, String title, boolean available, String message) {
}
```

This DTO is used specifically for the availability response.

Example JSON response:

```json
{
  "id": 1,
  "title": "Clean Code",
  "available": true,
  "message": "Book is available"
}
```

---

## 8.3 ApiError

```java
public record ApiError(String error, String message) {
}
```

Structured error payload returned by the API in error cases.

Example:

```json
{
  "error": "Book not found",
  "message": "No book found with id 999"
}
```

---

## 9. Exception Handling Design

## 9.1 InvalidBookIdException

```java
public class InvalidBookIdException extends RuntimeException {
    public InvalidBookIdException(String message) {
        super(message);
    }
}
```

Used when a path parameter like `/api/library/books/0` or `/api/library/books/-1` is invalid.

---

## 9.2 BookNotFoundException

```java
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }
}
```

Used when the gRPC service says the book is not found.

---

## 9.3 BookServiceUnavailableException

```java
public class BookServiceUnavailableException extends RuntimeException {
    public BookServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Used when the gRPC server cannot be reached or the call times out.

---

## 9.4 ApiExceptionHandler

File:

- LibraryService/src/main/java/com/example/libraryservice/ApiExceptionHandler.java

The class uses Spring `@RestControllerAdvice` to centralize exception handling.

```java
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
```

This ensures every error follows a consistent structure.

The HTTP statuses are:

- 404 for book not found
- 400 for invalid ID
- 503 for book service unavailable
- 500 for unexpected errors

---

## 10. End-to-End Request Flow

## 10.1 List all books

Request:

```http
GET /api/library/books
```

Flow:

1. HTTP request hits `LibraryController.listBooks()`
2. This calls `BookGrpcClient.listBooks()`
3. `BookGrpcClient` creates an empty `ListBooksRequest`
4. gRPC stub calls `BookService/ListBooks`
5. BookService returns all books from its in-memory map
6. `BookGrpcClient` converts each `Book` to `BookDto`
7. `LibraryController` returns JSON array

Response example:

```json
[
  {
    "id": 1,
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884",
    "available": true
  },
  {
    "id": 2,
    "title": "Effective Java",
    "author": "Joshua Bloch",
    "isbn": "9780134685991",
    "available": true
  }
]
```

---

## 10.2 Get one book

Request:

```http
GET /api/library/books/2
```

Flow:

1. `LibraryController.getBook(2)` is called
2. `validateId(2)` allows the value
3. `BookGrpcClient.getBook(2)` sends `GetBookRequest` with id = 2
4. Server searches the `books` map
5. If found, `GetBookResponse` contains `book`, `found = true`, `message = "Book found"`
6. Client converts the protobuf Book to `BookDto`
7. HTTP response is returned as JSON

Example response:

```json
{
  "id": 2,
  "title": "Effective Java",
  "author": "Joshua Bloch",
  "isbn": "9780134685991",
  "available": true
}
```

If not found:

- gRPC returns `found = false`
- BookGrpcClient throws `BookNotFoundException`
- `ApiExceptionHandler` returns HTTP 404 JSON error

Example error:

```json
{
  "error": "Book not found",
  "message": "No book found with id 999"
}
```

---

## 10.3 Availability check

Request:

```http
GET /api/library/books/3/availability
```

Flow:

1. `validateId(3)` passes
2. `BookGrpcClient.getBook(3)` fetches the book
3. `AvailabilityDto` is built from `BookDto`
4. It includes:
   - id
   - title
   - available
   - message text

Response example:

```json
{
  "id": 3,
  "title": "Head First Java",
  "available": false,
  "message": "Book is currently unavailable"
}
```

---

## 11. Error Handling Matrix

| Scenario | Where Happens | Result |
|---|---|---|
| ID <= 0 | LibraryController.validateId | InvalidBookIdException -> 400 |
| Book not found | BookGrpcClient.getBook() -> response.getFound() == false | BookNotFoundException -> 404 |
| gRPC server unreachable | BookGrpcClient catches StatusRuntimeException UNAVAILABLE/DEADLINE_EXCEEDED | BookServiceUnavailableException -> 503 |
| Unexpected issue | ApiExceptionHandler generic exception handler | 500 |

---

## 12. Design Decisions and Why They Were Chosen

### 12.1 Why gRPC?

- lightweight binary payload compared to JSON
- strong typing through proto
- better for internal service-to-service communication
- supports service contracts better than ad-hoc JSON modeling

### 12.2 Why a blocking stub?

- simpler to implement for this demo
- request/response use case fits blocking mode well
- easier for learning and explanation

### 12.3 Why REST in front of gRPC?

- REST is friendly for direct external clients and browsers
- the project demonstrates a common integration layer pattern
- REST can be used as a public API while gRPC powers internal communication

### 12.4 Why in-memory storage?

- this is a sample project
- it keeps setup simple and easy to understand
- no database or persistence layer is required for learning

---

## 13. Concurrency and Threading Model

The project is simple and mostly synchronous.

- BookService uses a shared in-memory map.
- gRPC server handles requests in threads managed by gRPC runtime.
- LibraryService uses blocking stub calls, so each request waits synchronously.

In a real production system, this would need:

- threadpool tuning
- connection pooling considerations
- load balancing
- retries
- timeouts and circuit breakers

---

## 14. Dependency and Build Setup

### BookService pom.xml highlights

Dependencies include:

- spring-boot-starter-web
- grpc-netty-shaded
- grpc-protobuf
- grpc-stub
- protobuf-java
- spring-boot-starter-test

Important build configuration:

- protobuf-maven-plugin is configured
- protoc and grpc-java plugins are used
- generated code is produced during Maven build

This is the reason the generated gRPC Java classes exist without being written manually.

### LibraryService pom.xml highlights

The same set of gRPC libraries are included, enabling the client to use generated stubs and protobuf messages.

---

## 15. What is Generated Automatically

After Maven compiles the .proto file, generated classes include:

- Book.java
- GetBookRequest.java
- GetBookResponse.java
- ListBooksRequest.java
- ListBooksResponse.java
- BookServiceGrpc.java
- BookServiceGrpc.BookServiceBlockingStub
- BookServiceGrpc.BookServiceImplBase

Those Java classes are used by both the server implementation and the client stub.

---

## 16. Sequence Diagram (Text Form)

### 16.1 GetBook flow

```text
Client
  -> LibraryService: GET /api/library/books/{id}
  -> LibraryController: getBook(id)
  -> BookGrpcClient: getBook(id)
  -> BookServiceGrpc.BlockingStub: getBook(GetBookRequest)
  -> BookService: getBook(request)
  -> BookGrpcService: lookup in books map
  -> BookGrpcService: build GetBookResponse
  <- BookService: GetBookResponse
  <- BookGrpcClient: BookDto
  <- LibraryController: JSON response
  <- Client: HTTP response body
```

### 16.2 ListBooks flow

```text
Client
  -> LibraryService: GET /api/library/books
  -> LibraryController: listBooks()
  -> BookGrpcClient: listBooks()
  -> BookServiceGrpc.BlockingStub: listBooks(ListBooksRequest)
  -> BookService: listBooks(request)
  -> BookGrpcService: read all books
  -> BookGrpcService: build ListBooksResponse
  <- BookService: ListBooksResponse
  <- BookGrpcClient: List<BookDto>
  <- LibraryController: JSON array
  <- Client: HTTP response
```

---

## 17. Data Model Summary

### BookService data model

In-memory map:

```java
private final Map<Integer, Book> books = new LinkedHashMap<>();
```

Example entries:

- 1 -> Clean Code
- 2 -> Effective Java
- 3 -> Head First Java
- 4 -> Spring in Action
- 5 -> Java: The Complete Reference

### REST data model

```java
record BookDto(int id, String title, String author, String isbn, boolean available)
record AvailabilityDto(int id, String title, boolean available, String message)
```

These are transformed, simplified representations used by the REST layer.

---

## 18. Current Limitations

This sample is intentionally simple and is not production-grade. The implementation currently has the following limitations:

- in-memory storage only
- no database persistence
- no retries or circuit breakers
- no load balancing or service discovery
- no TLS or authentication
- no distributed tracing/log aggregation
- no health endpoint and actuator integration
- no message queue or event-driven architecture

These limitations are acceptable because the project is a sample for learning gRPC and Spring Boot microservice communication.

---

## 19. Suggested Production Enhancements

If this system were to evolve beyond a demo, the next improvements would be:

- add a database (MySQL/Postgres)
- add JPA repositories for persistence
- add Spring Cloud Config or external configuration
- add service discovery (Eureka, Consul, Kubernetes service discovery)
- add TLS for secure gRPC
- add retries and circuit breaker patterns
- add health checks and metrics
- add logging and tracing with Zipkin or OpenTelemetry
- add Docker/Kubernetes deployment files

---

## 20. Final Design Summary

The system is built around the following simple but effective pattern:

- BookService owns the business data and exposes it through gRPC.
- LibraryService acts as the front-end application and REST API layer.
- The proto contract is the single source of truth.
- Spring Boot wires the components and manages startup lifecycle.
- gRPC performs communication between the two services.
- REST endpoints expose simplified business data to clients.

This is a clean example of a contract-first microservice design using Spring Boot and gRPC.

---

End of Low Level Design.
