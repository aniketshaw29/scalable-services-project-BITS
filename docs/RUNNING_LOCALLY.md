# Running Locally

Run all Campus EventHub services on your machine without Docker.
All services fall back to `localhost` defaults automatically — no environment variables required.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17+ | `java -version` to check |
| Maven | 3.8+ | `mvn -version` to check |
| PostgreSQL | 13+ | Running on `localhost:5432`, user `postgres`, password `postgres` |
| RabbitMQ | 3.x | Running on `localhost:5672`, user `guest`, password `guest` |

**PostgreSQL and RabbitMQ can be run via Docker if you prefer not to install them natively:**

```bash
docker run -d --name postgres \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine

docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.12-management
```

---

## Step 1 — Create Databases

PostgreSQL creates users and tables automatically via Hibernate, but the **databases must exist first**.

Connect to PostgreSQL and run:

```sql
CREATE DATABASE event_db;
CREATE DATABASE registration_db;
CREATE DATABASE venue_db;
CREATE DATABASE attendance_db;
CREATE DATABASE ticket_db;
CREATE DATABASE notification_db;
CREATE DATABASE certificate_db;
CREATE DATABASE feedback_db;
CREATE DATABASE leaderboard_db;
CREATE DATABASE announcement_db;
CREATE DATABASE resource_db;
CREATE DATABASE sponsor_db;
```

One-liner using `psql`:

```bash
for db in event_db registration_db venue_db attendance_db ticket_db notification_db \
           certificate_db feedback_db leaderboard_db announcement_db resource_db sponsor_db; do
  psql -U postgres -c "CREATE DATABASE $db;"
done
```

---

## Step 2 — Build All Services

From the project root:

```bash
mvn clean package -DskipTests
```

This builds all 14 services in one pass. JARs land in `<service>/target/*.jar`.

---

## Step 3 — Start Services

Services must start in dependency order. Open a terminal for each (or use a terminal multiplexer like `tmux`).

### Required order

```
1. eureka-server        (everything registers here)
2. api-gateway          (routes to registered services)
3. event-service        (registration-service depends on it)
4. venue-service        (registration-service depends on it)
5. registration-service
6. attendance-service
7. ticket-service
8. notification-service
9. certificate-service
10. feedback-service
11. leaderboard-service
12. announcement-service
13. resource-service
14. sponsor-service
```

### Start each service

```bash
java -jar eureka-server/target/*.jar
java -jar api-gateway/target/*.jar
java -jar event-service/target/*.jar
java -jar venue-service/target/*.jar
java -jar registration-service/target/*.jar
java -jar attendance-service/target/*.jar
java -jar ticket-service/target/*.jar
java -jar notification-service/target/*.jar
java -jar certificate-service/target/*.jar
java -jar feedback-service/target/*.jar
java -jar leaderboard-service/target/*.jar
java -jar announcement-service/target/*.jar
java -jar resource-service/target/*.jar
java -jar sponsor-service/target/*.jar
```

Or use `mvn spring-boot:run` from inside each service directory during development:

```bash
cd event-service && mvn spring-boot:run
```

### Tip: run all with a script

```bash
#!/usr/bin/env bash
# start-all.sh — run from project root
# Adjust sleep times if services are slow to start on your machine

services=(
  "eureka-server"
  "api-gateway"
  "event-service"
  "venue-service"
  "registration-service"
  "attendance-service"
  "ticket-service"
  "notification-service"
  "certificate-service"
  "feedback-service"
  "leaderboard-service"
  "announcement-service"
  "resource-service"
  "sponsor-service"
)

for svc in "${services[@]}"; do
  echo "Starting $svc..."
  java -jar "$svc/target/"*.jar > "logs/$svc.log" 2>&1 &
  sleep 3
done

echo "All services started. Check logs/ for output."
```

```bash
mkdir -p logs
chmod +x start-all.sh
./start-all.sh
```

---

## Step 4 — Verify

Once everything is running:

| Check | URL |
|-------|-----|
| Eureka dashboard (all services registered) | http://localhost:4070 |
| API Gateway health | http://localhost:4069/actuator/health |
| RabbitMQ management UI | http://localhost:15672 (guest / guest) |

---

## Running Tests

Run all tests across every service:

```bash
mvn test
```

Run tests for a single service:

```bash
cd event-service && mvn test
```

Tests use an H2 in-memory database and mock out Eureka, Feign clients, and RabbitMQ — no live infrastructure needed.

---

## Developing a Single Service

You don't need all 14 services running during development.
The minimum set depends on what you're working on:

| Service | Minimum dependencies to run |
|---------|----------------------------|
| event-service | eureka-server |
| venue-service | eureka-server |
| registration-service | eureka-server, event-service, venue-service |
| attendance-service | eureka-server, registration-service, rabbitmq |
| ticket-service | eureka-server, registration-service, rabbitmq |
| certificate-service | eureka-server, rabbitmq |
| notification-service | eureka-server, rabbitmq |
| feedback-service | eureka-server |
| leaderboard-service | eureka-server, rabbitmq |
| announcement-service | eureka-server, rabbitmq |
| resource-service | eureka-server |
| sponsor-service | eureka-server |

---

## Port Reference

| Service | Port |
|---------|------|
| API Gateway | 4069 |
| Eureka Server | 4070 |
| Event Service | 4071 |
| Registration Service | 4072 |
| Venue Service | 4073 |
| Attendance Service | 4074 |
| Ticket Service | 4075 |
| Notification Service | 4076 |
| Certificate Service | 4077 |
| Feedback Service | 4078 |
| Leaderboard Service | 4079 |
| Announcement Service | 4080 |
| Resource Service | 4081 |
| Sponsor Service | 4082 |
| PostgreSQL | 5432 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Management | 15672 |
