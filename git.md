# BookLibraryGrpcSystem - How to Run the Project

This repo contains two Spring Boot microservices and a minimal React UI for testing the gRPC-based flow.

## Architecture

- BookService: gRPC server that serves book data
  - Port: 8081
  - gRPC port: 9090 (implicit from the client config)
- LibraryService: REST API gateway that calls the BookService over gRPC
  - Port: 8080
- React UI: minimal testing interface for the client application
  - Port: 5173

## Prerequisites

- Java 17 or newer
- Maven 3.9+
- Node.js 18+
- npm 9+
- Git

## 1) Run the BookService (gRPC server)

```bash
cd BookService
mvn spring-boot:run
```

Expected result: the app starts on port 8081.

## 2) Run the LibraryService (REST facade over gRPC)

Open a new terminal and run:

```bash
cd LibraryService
mvn spring-boot:run
```

Expected result: the app starts on port 8080.

The client uses the gRPC target configured in:

- `LibraryService/src/main/resources/application.properties`

The service calls `localhost:9090` to reach the BookService gRPC endpoint.

## 3) Run the React UI

Open a new terminal and run:

```bash
cd react-ui
npm install
npm run dev -- --host 0.0.0.0
```

Then open:

```text
http://localhost:5173
```

## 4) Test the application through the UI

The UI provides a minimal page with the following actions:

- List all books
- Get a specific book by ID
- Check if a book is available
- View the API response in a JSON panel

Example requests the UI triggers:

- `GET http://localhost:8080/api/library/books`
- `GET http://localhost:8080/api/library/books/1`
- `GET http://localhost:8080/api/library/books/1/availability`

## 5) About the gRPC flow

This project demonstrates a classic gRPC pattern:

- The BookService implements the gRPC contract defined in `BookService/src/main/proto/book.proto`
- The LibraryService acts as the client and calls the BookService over gRPC
- The React UI does not talk to gRPC directly; it calls the LibraryService REST API, which hides the gRPC communication details

This makes the demo easier to test in a browser while still showing the gRPC concept in action.

## 6) Useful commands

Build BookService:

```bash
cd BookService
mvn clean package
```

Build LibraryService:

```bash
cd LibraryService
mvn clean package
```

Build the frontend:

```bash
cd react-ui
npm run build
```

## Troubleshooting

- If the UI shows errors, make sure both Java services are running first.
- If the LibraryService cannot reach BookService, confirm the gRPC target is `localhost:9090`.
- If the app fails to start, check Java version using `java -version`.
- If Node dependencies fail, delete `node_modules` and reinstall.

## Project folders

- `BookService/` – gRPC server implementation
- `LibraryService/` – REST client + gRPC client wrapper
- `react-ui/` – minimal React testing interface


