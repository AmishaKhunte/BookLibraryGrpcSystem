# Performance Testing Guide for This Project

This project includes two services:

- BookService: gRPC server on port 9090
- LibraryService: REST API on port 8080 that calls the BookService through gRPC

This guide shows how to do performance testing using free and open-source tools.

---

## 1. What to measure

For this architecture, test both layers:

1. BookService gRPC performance
   - request latency
   - throughput
   - p95 and p99 latency
   - error rate

2. LibraryService REST performance
   - HTTP request latency
   - throughput
   - success vs failure
   - dependency impact on BookService

The most relevant performance tests for this project are:

- GetBook RPC under concurrent load
- ListBooks RPC under concurrent load
- REST endpoints under load:
  - GET /api/library/books
  - GET /api/library/books/{id}
  - GET /api/library/books/{id}/availability

---

## 2. Best free tools for this project

## 2.1 ghz (best for gRPC)

ghz is a free open-source gRPC load testing tool.

Why it fits:

- simple command-line tool
- easy to run locally
- excellent for measuring gRPC latency and throughput
- supports concurrency and duration control
- very useful for BookService testing

## 2.2 k6 (best for REST and mixed workloads)

k6 is a free open-source load testing tool for HTTP and gRPC.

Why it fits:

- easy scripting in JavaScript
- good for testing REST endpoints
- works well for concurrent performance tests
- can test both HTTP and gRPC if needed

## 2.3 JMeter (free and mature)

JMeter is free and powerful.

Why it fits:

- good for HTTP benchmarking
- lots of plugins and metrics
- widely used in enterprise-style testing

Drawback:

- gRPC support is less direct than ghz or k6, so it is not the simplest for this repo

## 2.4 Apache Bench (ab) for very simple HTTP checks

This is a lightweight tool for rough HTTP performance testing.

It is useful for quick smoke tests, but not for advanced scripting or detailed results.

---

## 3. Recommended stack for this repository

Use this combination:

- ghz for BookService gRPC performance testing
- k6 for LibraryService REST performance testing
- optional JMeter for deeper HTTP test cases

This is the best free setup for this project.

---

## 4. Prerequisites

Before running any performance test:

1. Start BookService
2. Start LibraryService
3. Ensure no other service is bound to ports 8080 and 9090

Start BookService:

```bash
cd BookService
mvn spring-boot:run
```

Start LibraryService:

```bash
cd LibraryService
mvn spring-boot:run
```

Then verify the services are alive:

```bash
curl http://localhost:8080/actuator/health
```

and for gRPC:

```bash
grpcurl -plaintext localhost:9090 list
```

If `grpcurl` is not installed:

```bash
brew install grpcurl
```

or download the binary from the grpcurl GitHub releases.

---

## 5. Performance testing BookService gRPC with ghz

## 5.1 Install ghz

On macOS:

```bash
brew install ghz
```

On Linux, download the binary or use a package manager.

## 5.2 Basic test for GetBook

Test the `GetBook` RPC with moderate concurrency:

```bash
ghz --insecure \
  --proto BookService/src/main/proto/book.proto \
  --call booklibrary.BookService.GetBook \
  --data '{"id":1}' \
  --concurrency 20 \
  --total 1000 \
  localhost:9090
```

### Parameters explained

- `--insecure`: plaintext connection for local testing
- `--proto`: path to the proto file
- `--call`: RPC method to invoke
- `--data`: request payload
- `--concurrency`: how many concurrent workers
- `--total`: total number of requests
- `localhost:9090`: target server

### Example result

A result will include:

- total requests
- total latency
- requests per second
- p95 latency
- p99 latency
- success rate

This is the key output needed for bottleneck analysis.

## 5.3 Test ListBooks

```bash
ghz --insecure \
  --proto BookService/src/main/proto/book.proto \
  --call booklibrary.BookService.ListBooks \
  --data '{}' \
  --concurrency 20 \
  --total 1000 \
  localhost:9090
```

This measures the heavier list operation and is useful for comparing request size and complexity.

## 5.4 Increase concurrency for a realistic load

Try different loads to understand scalability:

```bash
ghz --insecure \
  --proto BookService/src/main/proto/book.proto \
  --call booklibrary.BookService.GetBook \
  --data '{"id":2}' \
  --concurrency 50 \
  --total 5000 \
  localhost:9090
```

Then compare:

- p95 latency
- requests/sec
- failure ratio

---

## 6. Performance testing LibraryService REST with k6

## 6.1 Install k6

On macOS:

```bash
brew install k6
```

On Linux, install from the k6 package source.

## 6.2 Create a simple test script

Create a file named `library-load.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 100 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  }
};

export default function () {
  const id = Math.floor(Math.random() * 5) + 1;

  let res = http.get(`http://localhost:8080/api/library/books/${id}`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(0.1);
}
```

This script sends concurrent requests to the LibraryService GET by ID endpoint.

## 6.3 Run the script

```bash
k6 run library-load.js
```

Results include:

- total requests
- request rate
- latency distribution
- p95 and p99 latency
- success/failure rate

## 6.4 Test the list endpoint

Another script could target:

```bash
http://localhost:8080/api/library/books
```

Example:

```javascript
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  vus: 20,
  duration: '60s'
};

export default function () {
  let res = http.get('http://localhost:8080/api/library/books');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
```

---

## 7. Use JMeter as a free option for REST performance testing

JMeter is also free and excellent for more detailed HTTP benchmarking.

### 7.1 Install JMeter

Download Apache JMeter from the official website.

### 7.2 Test plan design

Create a test plan with:

- Thread Group
- HTTP Request Defaults
- HTTP Request samplers
- View Results Tree
- Summary Report

Test endpoints:

- GET /api/library/books
- GET /api/library/books/1
- GET /api/library/books/1/availability

Set:

- 20, 50, 100 threads
- ramp-up period
- duration 1-5 minutes

Collect:

- average response time
- throughput
- error percentage
- min/max response time

This is useful if you want a GUI-based test suite for comparison and reporting.

---

## 8. Observability during performance tests

When running performance tests, monitor the services with:

- `/actuator/health`
- `/actuator/prometheus`
- Grafana dashboards
- JVM memory and CPU usage

This helps identify whether slowness is caused by:

- app CPU saturation
- memory pressure
- BookService timeouts
- gRPC connection limits
- HTTP bottlenecks in LibraryService

---

## 9. Suggested test scenarios

Use these load scenarios to evaluate the system:

## 9.1 Scenario 1: light load

- 5-10 concurrent users
- 30-second duration
- normal expected latency

## 9.2 Scenario 2: moderate load

- 20-50 concurrent users
- 1-minute duration
- check p95 latency and error rate

## 9.3 Scenario 3: stress test

- 100+ concurrent users
- 2-5 minutes duration
- observe breaking point and recovery

## 9.4 Scenario 4: gRPC dependency test

- run BookService under increasing concurrency with ghz
- watch LibraryService behavior when BookService slows down

---

## 10. Key metrics to collect

For each test, collect:

- total requests
- requests per second
- average latency
- p95 latency
- p99 latency
- error rate
- success ratio
- CPU usage
- memory usage
- response size

These values show whether the system is stable under load.

---

## 11. Useful thresholds for this project

For a small local project, a good target could be:

- p95 latency under 500 ms for REST endpoints
- p95 latency under 200-300 ms for simple gRPC GetBook
- error rate under 1%
- no timeouts under moderate load

These are local-development targets, not production SLA numbers.

---

## 12. Example performance test checklist

Before finalizing performance testing:

- start BookService and LibraryService
- run ghz for GetBook
- run ghz for ListBooks
- run k6 for GET /api/library/books
- run k6 for GET /api/library/books/{id}
- record all results in a spreadsheet or Markdown report
- compare latency under different concurrency levels
- identify the bottleneck

---

## 13. Practical guidance for this repo

For this specific repository, the most important performance test is the dependency chain:

```text
Client -> LibraryService REST -> BookService gRPC
```

So when testing, always look at both sides:

- the latency inside gRPC GetBook
- the impact on the REST API endpoint that calls it

This helps identify if the bottleneck is in:

- the REST layer
- the gRPC client
- the gRPC server
- the in-memory book data access

---

## 14. Summary

Best free performance testing setup for this project:

- ghz for gRPC load tests on BookService
- k6 for REST load tests on LibraryService
- optionally JMeter for deeper HTTP analysis
- Prometheus + Grafana to monitor runtime behavior during load

This combination is open-source, free, and practical for a Java Spring Boot gRPC microservice project.

---

## 15. Example commands summary

### BookService gRPC test

```bash
ghz --insecure \
  --proto BookService/src/main/proto/book.proto \
  --call booklibrary.BookService.GetBook \
  --data '{"id":1}' \
  --concurrency 20 \
  --total 1000 \
  localhost:9090
```

### LibraryService REST test with k6

```bash
k6 run library-load.js
```

### Check health endpoints

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:9090/actuator/health
```

---

End of performance testing guide.
