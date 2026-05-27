# Campus EventHub — Microservices-based College Event Management System

A production-grade microservices application built with Java + Spring Boot for managing college events end-to-end: from creation and venue assignment through registration, attendance, certificates, and feedback.

---

## Architecture Overview

```
                          ┌─────────────────────────────────────┐
                          │           API Gateway                │
                          │       (Spring Cloud Gateway)         │
                          │            Port: 4069                │
                          └──────────────┬──────────────────────┘
                                         │
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                           │
   ┌──────────▼────────┐    ┌────────────▼──────────┐  ┌───────────▼──────────┐
   │   Event Service   │    │ Registration Service  │  │   Venue Service      │
   │     Port: 4071    │    │     Port: 4072        │  │   Port: 4073         │
   └───────────────────┘    └───────────────────────┘  └──────────────────────┘
   ┌───────────────────┐    ┌───────────────────────┐  ┌──────────────────────┐
   │Attendance Service │    │ Ticket/QR Service     │  │ Notification Service │
   │     Port: 4074    │    │     Port: 4075        │  │   Port: 4076         │
   └───────────────────┘    └───────────────────────┘  └──────────────────────┘
   ┌───────────────────┐    ┌───────────────────────┐  ┌──────────────────────┐
   │Certificate Service│    │  Feedback Service     │  │ Leaderboard Service  │
   │     Port: 4077    │    │     Port: 4078        │  │   Port: 4079         │
   └───────────────────┘    └───────────────────────┘  └──────────────────────┘
   ┌───────────────────┐    ┌───────────────────────┐  ┌──────────────────────┐
   │Announcement Svc   │    │ Resource Upload Svc   │  │ Sponsor Mgmt Service │
   │     Port: 4080    │    │     Port: 4081        │  │   Port: 4082         │
   └───────────────────┘    └───────────────────────┘  └──────────────────────┘

                          ┌─────────────────────────────────────┐
                          │         Eureka Server                │
                          │       (Service Registry)             │
                          │            Port: 4070                │
                          └─────────────────────────────────────┘

                          ┌─────────────────────────────────────┐
                          │           RabbitMQ                   │
                          │       (Message Broker)               │
                          │     Port: 5672 / UI: 15672           │
                          └─────────────────────────────────────┘
```

---

## Services

| # | Service | Port | Responsibility | Database |
|---|---------|------|----------------|----------|
| 1 | Event Service | 4071 | Create/manage events | `event_db` |
| 2 | Registration Service | 4072 | Register students, track attendees | `registration_db` |
| 3 | Venue Service | 4073 | Manage venues, check availability | `venue_db` |
| 4 | Attendance Service | 4074 | Mark attendance, validate entry | `attendance_db` |
| 5 | Ticket/QR Service | 4075 | Generate & validate QR passes | `ticket_db` |
| 6 | Notification Service | 4076 | Send confirmations, reminders, alerts | `notification_db` |
| 7 | Certificate Service | 4077 | Generate & verify certificates | `certificate_db` |
| 8 | Feedback Service | 4078 | Ratings, comments, summaries | `feedback_db` |
| 9 | Leaderboard Service | 4079 | Rankings, competition results | `leaderboard_db` |
| 10 | Announcement Service | 4080 | Event announcements, broadcasts | `announcement_db` |
| 11 | Resource Upload Service | 4081 | File uploads for posters, notes | `resource_db` |
| 12 | Sponsor Service | 4082 | Sponsor details, tiers | `sponsor_db` |
| — | API Gateway | 4069 | Routing, load balancing | — |
| — | Eureka Server | 4070 | Service discovery | — |

---

## Communication

### Synchronous (REST via OpenFeign + Resilience4j)

```
Registration Service  ──► Event Service      (validate event exists, check capacity)
Registration Service  ──► Venue Service      (check venue availability)
Attendance Service    ──► Registration Service (validate student is registered)
Ticket Service        ──► Registration Service (validate registration before QR)
Certificate Service   ──► Attendance Service  (confirm attendance before cert)
```

### Asynchronous (RabbitMQ)

```
Registration completed  ──► Notification Service  (confirmation email/log)
Registration completed  ──► Ticket Service        (trigger QR generation)
Attendance completed    ──► Certificate Service   (trigger certificate generation)
Announcement created    ──► Notification Service  (broadcast announcement)
Results published       ──► Notification Service  (leaderboard notification)
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Service-to-Service | OpenFeign |
| Resilience | Resilience4j (Circuit Breaker, Retry, Rate Limiter) |
| Messaging | RabbitMQ |
| Database | PostgreSQL (separate DB per service) |
| QR Generation | ZXing (Zebra Crossing) |
| PDF/Certificate | Apache PDFBox or iText |
| Containerization | Docker + Docker Compose |
| Orchestration | Kubernetes |
| Build Tool | Maven (multi-module) |

---

## Project Structure

```
campus-eventhub/
├── pom.xml                          ← Parent POM
├── eureka-server/
├── api-gateway/
├── event-service/
├── registration-service/
├── venue-service/
├── attendance-service/
├── ticket-service/
├── notification-service/
├── certificate-service/
├── feedback-service/
├── leaderboard-service/
├── announcement-service/
├── resource-service/
├── sponsor-service/
├── docker-compose.yml
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── eureka/
│   ├── gateway/
│   └── services/
└── docs/
    ├── ARCHITECTURE.md
    ├── DEVELOPMENT_PLAN.md
    ├── API_CONTRACTS.md
    └── MESSAGE_CONTRACTS.md
```

---

## Quick Start (Docker Compose)

```bash
# Build all services
mvn clean package -DskipTests

# Start all infrastructure + services
docker-compose up --build

# Access points
# API Gateway:   http://localhost:4069
# Eureka UI:     http://localhost:4070
# RabbitMQ UI:   http://localhost:15672  (guest/guest)
```

## Quick Start (Kubernetes)

```bash
# Apply all manifests
kubectl apply -f k8s/

# Check pods
kubectl get pods -n campus-eventhub

# Port-forward gateway
kubectl port-forward svc/api-gateway 4069:4069 -n campus-eventhub
```

---

## Demo Flow

1. **Create Event** → `POST /api/events`
2. **Create Venue** → `POST /api/venues`
3. **Assign Venue to Event** → `PUT /api/venues/{id}/assign/{eventId}`
4. **Student Registers** → `POST /api/registrations`
   - Validates event capacity (sync → Event Service)
   - Validates venue availability (sync → Venue Service)
   - Publishes `registration.completed` event (async)
5. **QR Pass Generated** → Ticket Service consumes `registration.completed`
6. **Notification Sent** → Notification Service consumes `registration.completed`
7. **Mark Attendance** → `POST /api/attendance`
   - Validates registration (sync → Registration Service)
   - Publishes `attendance.completed` event (async)
8. **Certificate Generated** → Certificate Service consumes `attendance.completed`
9. **Submit Feedback** → `POST /api/feedback`
10. **Publish Leaderboard** → `POST /api/leaderboard/results`
    - Publishes `results.published` event (async)

---

## Failure Scenarios (Demo)

| Scenario | Expected Behavior |
|----------|------------------|
| Event Service down | Registration returns circuit breaker fallback response |
| Registration pod killed | Kubernetes restarts pod automatically |
| RabbitMQ queue backed up | Messages persist, consumed when service recovers |
| DB connection lost | Service returns 503, Resilience4j retry kicks in |

---

## Development Phases

See [DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md) for the full phased plan.

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | ✅ Done | Eureka Server + API Gateway |
| 2 | ✅ Done | Event Service (12 tests) + Venue Service (13 tests) |
| 3 | ✅ Done | Registration Service (14 tests) + Feign + Resilience4j circuit breaker |
| 4 | ✅ Done | Ticket Service (7 tests, ZXing QR) + Notification Service (9 tests) + RabbitMQ |
| 5 | 🔲 Next | Attendance Service + Certificate Service |
| 6 | 🔲 | Feedback + Leaderboard + Announcement |
| 7 | 🔲 | Resource Upload + Sponsor Service |
| 8 | 🔲 | Docker Compose containerization |
| 9 | 🔲 | Kubernetes deployment |
| 10 | 🔲 | Frontend Dashboard |
