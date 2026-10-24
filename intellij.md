# IntelliJ IDEA Setup Guide

This guide explains how to open, configure, and run this project in the latest IntelliJ IDEA version.

The repository contains two separate Spring Boot Maven modules:

- BookService
- LibraryService

Each module has its own `pom.xml` and can be run independently.

---

## 1. Prerequisites

Before opening the project in IntelliJ IDEA, make sure the following are installed:

- IntelliJ IDEA 2024 or newer (Community or Ultimate works)
- JDK 17
- Maven (bundled with IntelliJ is usually enough, but a system Maven install also works)
- Git

### Check Java version

Open a terminal and run:

```bash
java -version
```

It should show Java 17.

If not, install JDK 17 and set it as the default Java runtime on your machine.

---

## 2. Open the project in IntelliJ

1. Start IntelliJ IDEA.
2. Click `Open`.
3. Select the repository folder:

```text
BookLibraryGrpcSystem
```

4. IntelliJ will detect the Maven modules automatically.
5. If it asks how to import the project, choose:

- `Open as Project` or
- `Import project` if prompted

6. Let IntelliJ finish indexing and importing Maven dependencies.

---

## 3. Configure the Project SDK

If IntelliJ shows Java SDK issues:

1. Go to `File -> Project Structure`
2. Select `Project`
3. Set:
   - SDK: Java 17
   - Language level: 17
4. Click `Apply`

Also check:

- `File -> Project Structure -> Modules`
- Make sure both Maven modules are detected correctly

If Maven modules are not showing, reload them manually:

1. Open the Maven tool window (`View -> Tool Windows -> Maven`)
2. Click the `Reload All Maven Projects` button

---

## 4. Build the project

From IntelliJ, you can build using Maven or the IDE build tools.

Recommended first build:

```bash
mvn clean compile
```

Run this from the project root if you want to verify both Maven modules compile. You can also run it inside each service folder separately:

```bash
cd BookService
mvn clean compile
```

```bash
cd LibraryService
mvn clean compile
```

This also generates the gRPC classes from the `.proto` file.

---

## 5. Run BookService

The gRPC server is in the `BookService` module.

### 5.1 Create a run configuration

1. In IntelliJ, open the file:

```text
BookService/src/main/java/com/example/bookservice/BookServiceApplication.java
```

2. Right-click the file and choose `Run 'BookServiceApplication'`.
3. If IntelliJ asks to create a run configuration, confirm it.

### 5.2 Expected behavior

The application should start and launch the gRPC server on port `9090`.

You should see output similar to:

```text
Book Service gRPC server started on port 9090
```

### 5.3 Required startup order

Start `BookService` before `LibraryService`.

The `LibraryService` depends on the BookService gRPC server running at `localhost:9090`.

---

## 6. Run LibraryService

The REST API is in the `LibraryService` module.

### 6.1 Create a run configuration

1. Open the file:

```text
LibraryService/src/main/java/com/example/libraryservice/LibraryServiceApplication.java
```

2. Right-click it and choose `Run 'LibraryServiceApplication'`.

### 6.2 Expected behavior

The service should start on port `8080`.

This matches the configuration in:

```text
LibraryService/src/main/resources/application.properties
```

```properties
server.port=8080
spring.application.name=library-service
grpc.client.target=localhost:9090
```

So the app expects the `BookService` gRPC server to be available at `localhost:9090`.

---

## 7. Verify application behavior

Once both services are running:

### Test the REST API in the browser or Postman

Open these URLs in a browser or API client:

```text
http://localhost:8080/api/library/books
```

```text
http://localhost:8080/api/library/books/1
```

```text
http://localhost:8080/api/library/books/1/availability
```

You should receive JSON responses.

---

## 8. Troubleshooting

## 8.1 Import errors or unresolved Maven dependencies

Try:

1. `File -> Invalidate Caches / Restart`
2. `Maven -> Reload All Maven Projects`
3. Rebuild the project

Command line option:

```bash
mvn clean install
```

Run in each module as needed.

---

## 8.2 gRPC classes not found

This usually happens when the protobuf plugin has not generated classes yet.

Fix:

```bash
cd BookService
mvn clean compile
```

```bash
cd LibraryService
mvn clean compile
```

Then refresh IntelliJ.

---

## 8.3 BookService not reachable from LibraryService

Check these:

- BookService is running
- port 9090 is free
- no other service is already using 9090
- `grpc.client.target` is still `localhost:9090`

---

## 8.4 Spring Boot application not starting

Check:

- JDK 17 is configured in IntelliJ
- Maven project is imported correctly
- `pom.xml` is valid
- no syntax errors in Java files

---

## 9. Useful IntelliJ shortcuts

- `Ctrl+Shift+A` -> search IntelliJ actions
- `Ctrl+Alt+Shift+S` -> open project structure
- `Ctrl+F9` -> build project
- `Shift+F10` -> run selected config
- `Shift+F9` -> debug selected config

---

## 10. Recommended run order for this project

For development and testing, always use this order:

1. Start BookService
2. Wait for it to show the gRPC server started message
3. Start LibraryService
4. Test the REST endpoints

---

## 11. Summary

To set up this project in the latest IntelliJ IDEA:

1. Open the repository root
2. Import Maven project
3. Set JDK 17
4. Reload Maven modules
5. Run BookService first
6. Run LibraryService second
7. Test HTTP endpoints on port 8080

This is the easiest and most reliable way to work with this codebase in IntelliJ.

---

## 12. Optional command-line quick start

If you prefer running from terminal instead of IntelliJ:

```bash
cd BookService
mvn spring-boot:run
```

Then in another terminal:

```bash
cd LibraryService
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/api/library/books
```

---

End of IntelliJ setup guide.
