# Running Locally

Run all Campus EventHub services on your machine without Docker.
All Java services fall back to `localhost` defaults automatically — no environment variables required.
The React frontend dev server proxies `/api` calls to the gateway via Vite's built-in proxy.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | **21+** | `java -version` to check |
| Maven | 3.8+ | `mvn -version` to check |
| Node.js | **22+** | `node -v` to check — required for the frontend |
| npm | 10+ | Bundled with Node.js |
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

## Step 2 — Build All Java Services

From the project root:

```bash
mvn clean package -DskipTests
```

This builds all 14 services in one pass. JARs land in `<service>/target/*.jar`.

---

## Step 3 — Install Frontend Dependencies

```bash
cd frontend
npm install
cd ..
```

Only needed once (or after `package.json` changes).

---

## Step 4 — Start Services

Services must start in dependency order. Use the provided script or open a terminal per service.

### Option A — use start-all.sh (recommended)

```bash
./start-all.sh
```

On startup the script:
1. Verifies Java 21+ and Node.js 22+ are installed
2. **Checks PostgreSQL** — connects as `postgres`, verifies version ≥ 13, and confirms all 12 databases exist. Prints the exact `CREATE DATABASE` commands for any that are missing and exits.
3. **Checks RabbitMQ** — verifies port 5672 is reachable on `localhost`
4. Builds all 14 JARs (`mvn clean package -DskipTests`)
5. Starts all 14 Java services in dependency order (4-second gap each)
6. Starts the Vite dev server for the frontend

#### All flags

| Flag | Effect |
|------|--------|
| `--stop` | Stop all running services |
| `--stop <name>` | Stop one service by name (e.g. `--stop event-service`) |
| `--restart <name>` | Stop and restart one service without a full rebuild |
| `--status` | Show running/stopped status of every service with its PID |
| `--logs` | Tail all service logs (Ctrl+C to stop tailing — services keep running) |
| `--logs <name>` | Tail one service's log (e.g. `--logs registration-service`) |
| `--skip-build` | Skip `mvn package` — use existing JARs |
| `--skip-frontend` | Skip the React dev server |
| `--skip-infra-check` | Skip PostgreSQL and RabbitMQ pre-flight checks (useful when using Docker for infra) |

#### Examples

```bash
# Start everything (full pre-flight + build)
./start-all.sh

# Start without rebuilding (code unchanged)
./start-all.sh --skip-build

# Check what's running right now
./start-all.sh --status

# Restart only one service after a code change
./start-all.sh --restart event-service

# Stop one service
./start-all.sh --stop notification-service

# Watch a single service log
./start-all.sh --logs certificate-service

# Stop everything
./start-all.sh --stop

# Using Docker for PostgreSQL/RabbitMQ instead of native
./start-all.sh --skip-infra-check
```

### Option B — manual

Java start order:

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

```bash
java -jar eureka-server/target/*.jar
java -jar api-gateway/target/*.jar
java -jar event-service/target/*.jar
# ... etc
```

Then start the frontend dev server:

```bash
cd frontend
npm run dev
# Listening on http://localhost:3000
# /api/* proxied to http://localhost:4069
```

---

## Step 5 — Verify

Once everything is running:

| Check | URL |
|-------|-----|
| **Frontend** | http://localhost:3000 |
| Eureka dashboard (all services registered) | http://localhost:4070 |
| API Gateway health | http://localhost:4069/actuator/health |
| RabbitMQ management UI | http://localhost:15672 (guest / guest) |

---

## Running Tests

There are two test layers per service. Neither requires a running database, RabbitMQ, or Eureka — everything is in-process using H2.

**Layer 1 — Application tests** (`*ApplicationTests.java`): full HTTP endpoint coverage via MockMvc.

**Layer 2 — Integration tests** (`*IntegrationTest.java`): consumer-path tests (ticket, notification, certificate), service+repository wiring tests (feedback, leaderboard, announcement, attendance, sponsor).

See [docs/TESTING.md](TESTING.md) for the complete guide.

### Run everything

```bash
./test-all.sh
```

### Useful flags

```bash
./test-all.sh --fail-fast            # stop on first failure
./test-all.sh --service event-service   # one service only
```

### Single service with Maven

```bash
mvn -f event-service/pom.xml test
```

### Only integration tests

```bash
mvn -f ticket-service/pom.xml test -Dtest="*IntegrationTest"
```

### Only application tests

```bash
mvn -f ticket-service/pom.xml test -Dtest="*ApplicationTests"
```

### Single test method

```bash
mvn -f feedback-service/pom.xml test \
  -Dtest=FeedbackServiceIntegrationTest#summary_multipleRatings_avgCorrectlyComputed
```

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
| **frontend** | api-gateway (and whatever services you're testing against) |

For frontend-only work you can mock the backend by editing `src/api/` files to return static fixtures, or run only the services you need.

---

## Port Reference

| Service | Port |
|---------|------|
| **Frontend (dev server)** | **3000** |
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

<!-- steps tested on macOS and Ubuntu -->
