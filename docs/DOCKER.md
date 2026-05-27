# Docker Compose

Run the entire Campus EventHub stack — all 14 services, 12 databases, and RabbitMQ — with a single command.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Docker | 24+ |
| Docker Compose | v2 (bundled with Docker Desktop) |

Check versions:

```bash
docker --version
docker compose version
```

---

## Quick Start

```bash
# From the project root
docker compose up --build
```

First run takes several minutes — Maven downloads dependencies and builds all 14 JARs inside Docker.
Subsequent runs reuse the build cache and start much faster.

```bash
# Start without rebuilding (after first build)
docker compose up

# Run in background
docker compose up -d
```

---

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:4069 | — |
| Eureka Dashboard | http://localhost:4070 | — |
| RabbitMQ Management | http://localhost:15672 | guest / guest |

All API endpoints are accessed through the gateway at `http://localhost:4069`.

---

## Startup Order

Docker Compose uses `healthcheck` + `depends_on: condition: service_healthy` to enforce startup ordering:

```
rabbitmq           (healthcheck: rabbitmq-diagnostics ping)
  ↓
PostgreSQL DBs     (healthcheck: pg_isready — one per service)
  ↓
eureka-server      (healthcheck: curl /actuator/health)
  ↓
api-gateway        (depends on eureka healthy)
  ↓
All business services (depend on their DB + eureka; RabbitMQ services also depend on rabbitmq)
```

The full stack takes about 60–90 seconds to be fully operational on first start.

---

## Checking Service Health

```bash
# See all container statuses
docker compose ps

# Watch logs from all services
docker compose logs -f

# Logs from a specific service
docker compose logs -f event-service

# Check which services are registered in Eureka
curl http://localhost:4070/eureka/apps | grep -o '<app>.*</app>'
```

---

## Stopping and Cleaning Up

```bash
# Stop all containers (preserves volumes/data)
docker compose down

# Stop and delete all data volumes (fresh start)
docker compose down -v

# Remove built images too
docker compose down -v --rmi all
```

---

## Rebuilding a Single Service

After changing code in one service, rebuild and restart just that container:

```bash
docker compose up -d --build event-service
```

---

## Environment Variables

All services support environment variable overrides. The `docker-compose.yml` already sets the correct values for inter-container communication. To override for local customisation, create a `.env` file in the project root:

```env
# .env (optional overrides)
DB_USERNAME=myuser
DB_PASSWORD=mysecretpassword
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=adminpass
```

Docker Compose picks this up automatically.

---

## Volumes

Named Docker volumes persist data across container restarts:

| Volume | Contents |
|--------|----------|
| `event-db-data` | event_db PostgreSQL data |
| `registration-db-data` | registration_db data |
| `venue-db-data` | venue_db data |
| `attendance-db-data` | attendance_db data |
| `ticket-db-data` | ticket_db data |
| `notification-db-data` | notification_db data |
| `certificate-db-data` | certificate_db data |
| `feedback-db-data` | feedback_db data |
| `leaderboard-db-data` | leaderboard_db data |
| `announcement-db-data` | announcement_db data |
| `resource-db-data` | resource_db data |
| `sponsor-db-data` | sponsor_db data |
| `rabbitmq-data` | RabbitMQ queues and messages |
| `resource-uploads` | Uploaded files for resource-service |

---

## Failure Scenarios (Demo)

### Circuit Breaker

Stop the Event Service to trigger the circuit breaker in Registration Service:

```bash
docker compose stop event-service

# Try to register — should return 503 fallback
curl -X POST http://localhost:4069/api/registrations \
  -H "Content-Type: application/json" \
  -d '{"studentId":"STU-001","studentName":"Alice","studentEmail":"alice@college.edu","eventId":1}'

# Restart — circuit closes, service re-registers in Eureka
docker compose start event-service
```

### Pod Recovery

Kill and watch a service recover:

```bash
docker compose kill registration-service
docker compose up -d registration-service
docker compose logs -f registration-service
```

### RabbitMQ Message Persistence

Stop a consumer service, publish a message, restart — the message is waiting:

```bash
docker compose stop notification-service
# Create an announcement (triggers notification queue)
curl -X POST http://localhost:4069/api/announcements ...
# Restart consumer — it processes the queued message
docker compose start notification-service
docker compose logs -f notification-service
```

---

## Accessing Individual Containers

```bash
# Shell into any container
docker compose exec event-service sh

# Connect to a database
docker compose exec event-db psql -U postgres -d event_db

# List tables
docker compose exec event-db psql -U postgres -d event_db -c '\dt'
```

---

## Dockerfile Pattern

Every service uses the same two-stage Dockerfile:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (lean JRE only)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The runtime image is ~200MB vs ~500MB for a full JDK image.
