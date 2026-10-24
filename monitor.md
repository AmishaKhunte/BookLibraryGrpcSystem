# Monitoring gRPC Services with Free Tools

This project has two services:

- BookService: gRPC server
- LibraryService: REST API + gRPC client

A good free monitoring setup for this project is:

- Spring Boot Actuator for health and metrics
- Prometheus for collecting metrics
- Grafana for dashboards
- Jaeger or Zipkin for tracing
- grpcurl for manual gRPC health checks

This gives you visibility into:

- service availability
- request latency
- error counts
- gRPC call success/failure
- dependency health between LibraryService and BookService

---

## 1. Best free monitoring stack for this repo

Use this stack:

- Spring Boot Actuator + Micrometer
- Prometheus
- Grafana
- Jaeger or Zipkin
- grpcurl

This is the easiest and most practical free solution for a Java Spring Boot gRPC project.

---

## 2. What to monitor

For BookService, monitor:

- gRPC server health
- gRPC request count
- gRPC latency
- errors / failed requests
- JVM memory, GC, CPU
- thread usage

For LibraryService, monitor:

- HTTP request rate and latency
- gRPC client call success/failure
- BookService dependency availability
- HTTP error responses
- JVM metrics

---

## 3. Add Actuator and Prometheus metrics

Add these dependencies to both services in their `pom.xml` files.

### BookService

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### LibraryService

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

These dependencies expose metrics in Prometheus format and health endpoints.

---

## 4. Configure Actuator endpoints

Add this to each service's `application.properties`:

```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
management.metrics.tags.application=${spring.application.name}
management.prometheus.metrics.export.enabled=true
```

For BookService:

```properties
server.port=9090
spring.application.name=book-service
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
management.metrics.tags.application=${spring.application.name}
management.prometheus.metrics.export.enabled=true
```

For LibraryService:

```properties
server.port=8080
spring.application.name=library-service
grpc.client.target=localhost:9090
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
management.metrics.tags.application=${spring.application.name}
management.prometheus.metrics.export.enabled=true
```

This exposes:

- `/actuator/health`
- `/actuator/prometheus`
- `/actuator/metrics`

---

## 5. Health checks to use in practice

For each service, the health endpoint can be called directly:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:9090/actuator/health
```

A healthy response will look like:

```json
{
  "status": "UP"
}
```

This is a very useful free health check for monitoring dashboards or load balancers.

---

## 6. Prometheus setup (free and self-hosted)

Prometheus is one of the best free monitoring tools for Spring Boot and gRPC services.

### 6.1 Create a Prometheus config file

Create a file named `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'book-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:9090']

  - job_name: 'library-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

Notes:

- If running Prometheus on the same machine, `host.docker.internal` is usually useful for Docker-based local deployments.
- If running natively, replace with `localhost` or your machine IP.

### 6.2 Run Prometheus with Docker

```bash
docker run -d \
  --name prometheus \
  -p 9091:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

Then open:

```text
http://localhost:9091
```

Prometheus should show the targets and scrape status.

---

## 7. Grafana dashboard (free)

Grafana is free and excellent for dashboards.

### 7.1 Start Grafana with Docker

```bash
docker run -d \
  --name=grafana \
  -p 3000:3000 \
  grafana/grafana
```

Then open:

```text
http://localhost:3000
```

Default login:

- username: admin
- password: admin

Then change the password when prompted.

### 7.2 Add Prometheus as a data source

In Grafana:

1. Go to Configuration -> Data Sources
2. Add Prometheus
3. Set URL to:

```text
http://host.docker.internal:9091
```

or

```text
http://localhost:9091
```

### 7.3 Useful dashboards to create

Monitor these metrics:

- JVM memory
- CPU usage
- HTTP requests per second
- HTTP 5xx errors
- application uptime
- process uptime
- request latency
- gRPC client errors (when supported by gRPC instrumentation)

For this project, a basic dashboard should include:

- LibraryService HTTP request count
- BookService status / health
- LibraryService gRPC client call failures
- average response time
- JVM heap and thread usage

---

## 8. Trace requests with Jaeger or Zipkin (free)

For distributed tracing, use either Jaeger or Zipkin.

### Option A: Jaeger

```bash
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

Open:

```text
http://localhost:16686
```

### Option B: Zipkin

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

Open:

```text
http://localhost:9411
```

### Add tracing to Spring Boot

Add the dependency:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

Then configure:

```properties
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
management.tracing.enabled=true
management.tracing.sampling.probability=1.0
```

This allows you to trace a request from:

- HTTP request to LibraryService
- LibraryService gRPC call to BookService
- downstream response back to client

---

## 9. Check gRPC health directly with grpcurl

`grpcurl` is a free and very useful tool for testing gRPC health and RPC calls manually.

### Install grpcurl

On Linux/macOS:

```bash
brew install grpcurl
```

Or download binary from GitHub releases.

### Check service health

```bash
grpcurl -plaintext localhost:9090 list
```

Example output:

```text
booklibrary.BookService
```

### Call the methods manually

```bash
grpcurl -plaintext -d '{"id":1}' localhost:9090 booklibrary.BookService/GetBook
```

You can also call the list method:

```bash
grpcurl -plaintext localhost:9090 booklibrary.BookService/ListBooks
```

This is helpful for manual testing when the app is running but you want to confirm actual runtime health.

---

## 10. Add gRPC health checking in the server

If you want proper gRPC health checks, add the health service to BookService.

Example idea:

```java
HealthStatusManager healthStatusManager = new HealthStatusManager();
Server server = ServerBuilder.forPort(9090)
        .addService(new HealthService())
        .addService(bookGrpcService)
        .build();
```

This makes health checks easier for monitoring tools and orchestrators.

For a simple project, the Actuator and Prometheus endpoints are usually enough, but gRPC health checks are a good addition.

---

## 11. Suggested monitoring flow for this project

A practical flow is:

1. BookService exposes Actuator health and Prometheus metrics.
2. LibraryService exposes Actuator health and Prometheus metrics.
3. Prometheus scrapes both services.
4. Grafana visualizes these metrics.
5. Jaeger or Zipkin traces a request from the UI/API layer to the gRPC dependency.
6. grpcurl validates the gRPC contract manually.

This gives complete visibility with free tools only.

---

## 12. Minimal setup recommendation

If you want the simplest free monitoring setup for this repo:

- Actuator + Prometheus on both services
- Prometheus to scrape both services
- Grafana dashboard to visualize metrics
- grpcurl for direct gRPC checks

This is enough for most small projects and is easy to set up for free.

---

## 13. Example docker-compose stack

You can run the following three tools together with Docker Compose:

```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9091:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"

  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
```

This is free, easy, and works well in local development.

---

## 14. Monitoring checklist

Check the following before considering the system healthy:

- BookService `/actuator/health` returns UP
- LibraryService `/actuator/health` returns UP
- Prometheus shows both targets as UP
- Grafana query shows request counts and latency
- grpcurl can call `GetBook` and `ListBooks`
- Jaeger or Zipkin shows trace propagation between LibraryService and BookService

---

## 15. Summary

For this project, the best free monitoring strategy is:

- Spring Boot Actuator
- Prometheus
- Grafana
- Jaeger or Zipkin
- grpcurl

This gives you health, metrics, dashboards, tracing, and manual gRPC validation without paying for enterprise monitoring tools.

It is simple to deploy locally, works well with Spring Boot, and is a responsible free setup for a Java gRPC microservice project.

---

End of monitoring guide.
