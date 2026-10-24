# BatchService Setup Guide

This document explains how to set up and run the Spring Batch module in this project.

## Prerequisites

Before starting the batch service, make sure the following are installed:

- Java 17+
- Maven 3.9+
- Git

Check Java and Maven:

```bash
java -version
mvn -version
```

## 1) Open the repository root

```bash
cd BookLibraryGrpcSystem
```

## 2) Start the BatchService module

```bash
cd BatchService
mvn spring-boot:run
```

The service will start on:

```text
http://localhost:8082
```

## 3) Trigger the batch job

Use the REST endpoint to run the job:

```bash
curl -X POST http://localhost:8082/api/batch/run
```

Example response:

```json
{
  "jobName": "bookImportJob",
  "status": "COMPLETED",
  "exitCode": "COMPLETED"
}
```

## 4) Access the H2 database console

The application uses an in-memory H2 database.

Open this URL in a browser:

```text
http://localhost:8082/h2-console
```

Use these connection details:

```text
JDBC URL: jdbc:h2:mem:batchdb
User: sa
Password: 
```

## 5) Database table created by the batch job

The job imports data into the table:

```text
book_record
```

Fields:

- id
- title
- author
- isbn
- available

## 6) Input file used by the batch job

The batch job reads the CSV file:

```text
BatchService/src/main/resources/books.csv
```

Example content:

```csv
id,title,author,isbn,available
1,Clean Code,Robert C. Martin,9780132350884,true
2,Effective Java,Joshua Bloch,9780134685991,true
```

## 7) Batch job behavior

The job does the following:

- reads the CSV file
- maps each row to a `BookRecord`
- transforms it to a `BookEntity`
- writes the record into the H2 database

## 8) Build the project

To build the module without running it:

```bash
cd BatchService
mvn clean package
```

## 9) Troubleshooting

### Application fails to start

Check Java version:

```bash
java -version
```

Ensure Maven dependencies can download successfully.

### Job does not run

Make sure the application is up and the endpoint is called correctly:

```bash
curl -X POST http://localhost:8082/api/batch/run
```

### Database console is not accessible

Ensure the app is running and `spring.h2.console.enabled=true` is set in:

```text
BatchService/src/main/resources/application.properties
```

## 10) Project structure

```text
BatchService/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/batchservice/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── books.csv
```

This BatchService module is a simple Spring Batch example for importing book records from a CSV file into an H2 database.
