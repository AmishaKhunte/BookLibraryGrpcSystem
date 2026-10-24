# VS Code Setup Guide

This guide explains how to open, configure, and run this project in the latest Visual Studio Code version.

The repository contains two separate Spring Boot Maven modules:

- BookService
- LibraryService

Each module has its own `pom.xml` and can be run separately.

---

## 1. Prerequisites

Before opening the project in VS Code, make sure you have:

- Visual Studio Code latest version
- JDK 17
- Maven
- Git

### Check Java version

Open a terminal and run:

```bash
java -version
```

You should see Java 17.

If you do not have Java 17 installed, install it first.

---

## 2. Install recommended VS Code extensions

Open VS Code and install these extensions:

- Extension Pack for Java
- Spring Boot Extension Pack
- Maven for Java
- Java Test Runner
- Debugger for Java
- GitHub Pull Requests and Issues (optional)

You can install them from the Extensions tab in VS Code.

---

## 3. Open the repository in VS Code

1. Open VS Code.
2. Click `File -> Open Folder`.
3. Select the project folder:

```text
BookLibraryGrpcSystem
```

4. VS Code will load the repository.

If the Java extensions detect the Maven project, they will automatically configure the workspace for Java development.

---

## 4. Configure Java SDK in VS Code

If VS Code shows Java SDK errors:

1. Open the Command Palette:
   - `Ctrl+Shift+P`
2. Run:
   - `Java: Configure Java Runtime`
3. Select Java 17 as the project runtime.

You can also check the Java version from the Java extension status.

---

## 5. Import and refresh Maven projects

VS Code with the Java extension should detect the Maven structure automatically. If not:

1. Open the Command Palette.
2. Run:
   - `Maven: Reload Projects`

You can also open the Maven panel from the left activity bar.

If the project does not load correctly:

1. Open the terminal in the repo root.
2. Run:

```bash
mvn clean compile
```

This will validate the Maven setup before running the project.

---

## 6. Build the project

Build each service separately:

```bash
cd BookService
mvn clean compile
```

```bash
cd LibraryService
mvn clean compile
```

This also compiles the gRPC code generated from the `.proto` file.

---

## 7. Run BookService

### Option 1: Run through VS Code terminal

Open a terminal and run:

```bash
cd BookService
mvn spring-boot:run
```

### Option 2: Use the Run and Debug panel

1. Open the `Run and Debug` panel.
2. Click `create a launch.json file` if prompted.
3. Choose `Java` as the environment.
4. Select the main class:

```text
com.example.bookservice.BookServiceApplication
```

This starts the gRPC server on port `9090`.

Look for output similar to:

```text
Book Service gRPC server started on port 9090
```

---

## 8. Run LibraryService

Open a terminal and run:

```bash
cd LibraryService
mvn spring-boot:run
```

This starts the REST API on port `8080`.

The configuration is in:

```text
LibraryService/src/main/resources/application.properties
```

```properties
server.port=8080
spring.application.name=library-service
grpc.client.target=localhost:9090
```

So the app expects BookService to be running on `localhost:9090`.

---

## 9. Test the application

Once both services are running, call these endpoints:

```text
http://localhost:8080/api/library/books
http://localhost:8080/api/library/books/1
http://localhost:8080/api/library/books/1/availability
```

You can use the built-in browser, Postman, curl, or Swagger-like tools.

Example using curl:

```bash
curl http://localhost:8080/api/library/books
```

```bash
curl http://localhost:8080/api/library/books/1
```

---

## 10. Debugging in VS Code

### 10.1 Debug BookService

1. Open the `Run and Debug` panel.
2. Select the Java configuration for `BookServiceApplication`.
3. Click start.

### 10.2 Debug LibraryService

1. Open the `Run and Debug` panel.
2. Select the Java configuration for `LibraryServiceApplication`.
3. Click start.

You can also add breakpoints in:

- BookGrpcService
- LibraryController
- BookGrpcClient

---

## 11. Common problems and fixes

## 11.1 Java extension not detecting the project

Try:

- reopen the folder
- run `Maven: Reload Projects`
- install the Java Extension Pack

---

## 11.2 Maven dependencies not resolving

Run:

```bash
mvn clean install
```

In the relevant module directory.

---

## 11.3 gRPC classes not generated

This happens when the `.proto` file does not compile yet.

Run:

```bash
cd BookService
mvn clean compile
```

Then:

```bash
cd LibraryService
mvn clean compile
```

---

## 11.4 LibraryService cannot connect to BookService

Check the following:

- BookService is running
- the gRPC server on port `9090` is alive
- `grpc.client.target=localhost:9090` is still correct
- no other local process is using port 9090

---

## 12. Recommended startup order

Always start the services in this order:

1. BookService
2. LibraryService

This keeps the gRPC connection working correctly.

---

## 13. VS Code workspace suggestions

A simple workspace structure is enough:

```text
BookLibraryGrpcSystem/
  BookService/
  LibraryService/
```

No special workspace file is required for this project.

---

## 14. Helpful commands

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

Run BookService:

```bash
cd BookService
mvn spring-boot:run
```

Run LibraryService:

```bash
cd LibraryService
mvn spring-boot:run
```

---

## 15. Final Summary

To run this project in VS Code:

1. Install Java 17 and VS Code Java extensions
2. Open the repository folder
3. Reload Maven projects
4. Run BookService first
5. Run LibraryService second
6. Test the REST APIs on port 8080

This is the simplest setup for the current codebase.

---

End of VS Code setup guide.
