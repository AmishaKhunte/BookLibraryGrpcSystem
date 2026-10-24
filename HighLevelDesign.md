# High Level Design (HLD) - BookLibraryGrpcSystem

## 1. Overview

BookLibraryGrpcSystem is a sample Spring Boot microservices system built around gRPC communication between two services:

- BookService: a backend gRPC service that stores and exposes book catalog data.
- LibraryService: a Spring Boot REST API service that calls BookService over gRPC and exposes business-friendly HTTP endpoints to clients.

The system demonstrates a common microservice pattern where:
- one service owns a domain capability (book catalog),
- another service consumes it through a strongly typed RPC contract,
- clients interact with the system through a REST API layer.

---

## 2. Business Objective

The application provides a simple library domain:
- list all books
- fetch a book by ID
- check whether a book is available

It aims to demonstrate:
- Spring Boot application structure
- gRPC service contract definition with Protocol Buffers
- inter-service communication using gRPC
- REST exposure in front of a gRPC backend

---

## 3. High-Level Architecture

The solution has a two-service architecture:

1. BookService
   - Spring Boot application
   - Starts a gRPC server on port 9090
   - Implements the BookService RPC contract
   - Maintains an in-memory set of books

2. LibraryService
   - Spring Boot REST application
   - Exposes HTTP endpoints under /api/library/books
   - Uses a gRPC blocking client to call BookService
   - Maps gRPC responses into HTTP-friendly DTOs

Interaction:

Client -> LibraryService REST API -> BookGrpcClient -> gRPC stub -> BookService gRPC server -> proto-defined service -> response -> LibraryService -> HTTP response

---

## 4. Components

### 4.1 BookService

This service is responsible for the core domain data.

Key files and responsibilities:
- BookServiceApplication
  - bootstrap class for the Spring Boot app
- GrpcServerLifecycle
  - starts the gRPC server on port 9090
  - registers the BookGrpcService implementation
- BookGrpcService
  - implements the generated gRPC service methods:
    - GetBook
    - ListBooks
  - keeps an in-memory book map with sample catalog entries

#### Responsibilities
- serving data for book lookup
- returning list of books
- validating business data through the proto contract

#### Communication
- gRPC server available on localhost:9090

---

### 4.2 LibraryService

This service acts as the API facade and gRPC consumer.

Key files and responsibilities:
- LibraryServiceApplication
  - bootstrap class for the Spring Boot app
- LibraryController
  - exposes REST endpoints:
    - GET /api/library/books
    - GET /api/library/books/{id}
    - GET /api/library/books/{id}/availability
- BookGrpcClient
  - calls BookService methods using generated gRPC stubs
  - handles deadlines and service unavailability
- GrpcClientConfiguration
  - creates the managed gRPC channel
  - configures the target host and port
- DTO classes
  - BookDto
  - AvailabilityDto
  - exception classes and handler classes

#### Responsibilities
- expose HTTP APIs to external clients
- translate HTTP requests into the gRPC contract
- map gRPC responses to business objects
- handle errors gracefully

---

## 5. API Contract

The contract is defined in the proto file:

- package: booklibrary
- service: BookService
- methods:
  - GetBook(GetBookRequest) returns (GetBookResponse)
  - ListBooks(ListBooksRequest) returns (ListBooksResponse)

### Data model
The proto defines:
- Book
  - id
  - title
  - author
  - isbn
  - available
- GetBookRequest
  - id
- GetBookResponse
  - book
  - found
  - message
- ListBooksRequest
- ListBooksResponse
  - repeated Book books

This gives the system a strongly typed contract that both services share and generated code uses.

---

## 6. Runtime Flow

### 6.1 List books

Sequence:
1. Client calls GET /api/library/books
2. LibraryController receives the request
3. BookGrpcClient calls listBooks on the gRPC stub
4. BookService gRPC server responds with ListBooksResponse
5. LibraryService converts results to BookDto list
6. REST response is returned to the client

### 6.2 Get a single book

Sequence:
1. Client calls GET /api/library/books/{id}
2. LibraryController validates the ID
3. BookGrpcClient calls GetBook with the requested ID
4. BookService returns GetBookResponse
5. If not found, LibraryService raises a domain-specific exception
6. If found, DTO is returned in the HTTP response

### 6.3 Availability check

Sequence:
1. Client calls GET /api/library/books/{id}/availability
2. LibraryController validates the book ID
3. BookGrpcClient fetches the book data via gRPC
4. LibraryService builds an AvailabilityDto
5. Response contains:
   - book id
   - book title
   - availability flag
   - message text

---

## 7. Technology Stack

### BookService
- Java
- Spring Boot
- gRPC
- Protocol Buffers
- in-memory data store

### LibraryService
- Java
- Spring Boot
- Spring Web
- gRPC client
- REST controllers
- DTOs and exception handling

---

## 8. Configuration

The client target is configured in:

- LibraryService/src/main/resources/application.properties

Key configuration:
- server.port=8080
- spring.application.name=library-service
- grpc.client.target=localhost:9090

This means:
- the REST API is exposed on port 8080
- the gRPC backend is expected on localhost:9090

The BookService gRPC server is launched internally via GrpcServerLifecycle and listens on port 9090.

---

## 9. Design Patterns Used

### 9.1 Service Layer Pattern
The Business logic is isolated in service-like components such as:
- BookGrpcService
- BookGrpcClient

### 9.2 DTO Layer Pattern
The REST layer uses DTOs instead of exposing gRPC-generated model objects directly.

### 9.3 API Gateway-like Facade
LibraryService acts as an API facade in front of a gRPC dependency.

### 9.4 Exception Handling
LibraryService uses custom exceptions and a centralized exception handler for clean error responses.

---

## 10. Data Handling

The system currently uses an in-memory map for book storage in BookGrpcService:

- Book ids are integers
- Each book has title, author, isbn, and availability
- Sample data is preloaded into memory during service startup

This is suitable for a demo or sample service but not for production durability.

---

## 11. Non-Functional Considerations

### Performance
- gRPC uses HTTP/2 and binary serialization, reducing payload size and improving efficiency.
- This is beneficial for service-to-service communication.

### Reliability
- BookGrpcClient adds deadlines to gRPC requests:
  - 2-second deadline
- Unavailable or timed-out service calls are mapped to a domain-specific exception.

### Maintainability
- Proto contract provides a single source of truth for service API definitions.
- Generated stubs minimize mismatch issues across services.

### Scalability
- The architecture supports additional microservices and more domain services.
- gRPC communication is ideal for internal microservice traffic.

---

## 12. Security and Production Readiness

This sample implementation is a learning/demo project and does not include:
- authentication
- authorization
- TLS
- API gateway protection
- database persistence
- service discovery
- centralized config
- distributed tracing
- health checks
- logging and monitoring

For production, additional concerns should be addressed:
- secure gRPC transport
- service registry and discovery
- external database
- retry and circuit breaker logic
- observability stack

---

## 13. Deployment View

The conceptual deployment is simple:

- BookService deployed as a backend service
- LibraryService deployed as REST API service
- Both run in the same local environment in the sample configuration
- BookService listens on 9090
- LibraryService exposes HTTP REST on 8080

---

## 14. Summary

This project demonstrates a practical Spring Boot microservices design using gRPC for backend communication and REST for external access.

Highlights:
- BookService owns the book data and exposes it via gRPC
- LibraryService consumes the data via generated gRPC client stubs
- The API contract is shared using .proto definitions
- REST endpoints provide a friendly external interface
- The design shows how microservices can be decoupled while staying strongly typed and efficient

This is a good reference architecture for:
- service-to-service communication using gRPC
- Spring Boot-based microservice integration
- contract-first design with protobuf
- building a thin API layer over a backend gRPC service

---

End of High Level Design.
