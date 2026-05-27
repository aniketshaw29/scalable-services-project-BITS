# Registration Service

**Port:** 4072
**Database:** `registration_db`
**Role:** Enroll students in events. Enforces capacity limits and publishes async events downstream.

---

## Overview

Registration Service is the most connected service in the platform. It makes **synchronous Feign calls** to validate events and venues before allowing a registration, enforces a **circuit breaker** so a crashed Event Service doesn't take it down, and **publishes a RabbitMQ message** on successful registration to trigger QR ticket generation and notification.

---

## Domain Model

### Entity: `Registration`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `studentId` | String | Unique student identifier |
| `studentName` | String | Required |
| `studentEmail` | String | Required |
| `eventId` | Long | Event being registered for |
| `registeredAt` | LocalDateTime | |
| `status` | Enum | `ACTIVE`, `CANCELLED` |

**Unique constraint:** `(student_id, event_id)` — one registration per student per event.

---

## API Endpoints

### Register Student
```
POST /api/registrations
Content-Type: application/json

{
  "studentId": "STU-001",
  "studentName": "Alice Johnson",
  "studentEmail": "alice@college.edu",
  "eventId": 1
}

201 Created: { "id": 1, "studentId": "STU-001", "status": "ACTIVE", ... }
409 Conflict: student already registered for this event
409 Conflict: event is at full capacity
503 Service Unavailable: event service circuit breaker open
```

### Get Registration by ID
```
GET /api/registrations/{id}

200 OK: { ...registration }
404 Not Found
```

### List Registrations for Event
```
GET /api/registrations/event/{eventId}

200 OK: [ { ...registration }, ... ]
```

### Student's Registrations
```
GET /api/registrations/student/{studentId}

200 OK: [ { ...registration }, ... ]
```

### Cancel Registration
```
DELETE /api/registrations/{id}

204 No Content
404 Not Found
```

### Check Registration Exists (internal — called by Attendance and Ticket services)
```
GET /api/registrations/{id}/exists

200 OK: { "exists": true, "status": "ACTIVE" }
200 OK: { "exists": false }
```

---

## Synchronous Dependencies (Feign)

### EventClient → Event Service
Called when registering to:
1. Verify the event exists
2. Check `currentRegistrations < maxCapacity`
3. Increment capacity (`PUT /api/events/{id}/capacity`)

**Fallback:** Returns 503 with message `"Event service is currently unavailable. Please try again later."`

### VenueClient → Venue Service
Called to confirm a venue has been assigned to the event (`GET /api/venues/event/{eventId}`).

**Fallback:** Returns 503 with message `"Venue service is currently unavailable."`

### Circuit Breaker (Resilience4j)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      event-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
  retry:
    instances:
      event-service:
        max-attempts: 3
        wait-duration: 500ms
```

---

## Asynchronous Publishing (RabbitMQ)

On successful registration, publishes to exchange `campus.events` with routing key `registration.completed`:

```json
{
  "registrationId": 1,
  "studentId": "STU-001",
  "studentName": "Alice Johnson",
  "studentEmail": "alice@college.edu",
  "eventId": 1,
  "eventTitle": "Spring Boot Workshop",
  "registeredAt": "2024-02-15T10:00:00"
}
```

**Consumers:**
- Ticket Service → generates QR pass
- Notification Service → logs confirmation notification

---

## Configuration

```yaml
server:
  port: 4072
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/registration_db}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:4070/eureka/}
feign:
  circuitbreaker:
    enabled: true
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/registration_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ broker host |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `guest` | |
| `RABBITMQ_PASSWORD` | `guest` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

14 automated integration tests covering:
- Register student (success)
- Duplicate registration (409)
- Event at capacity (409)
- Cancel registration
- Get by ID, by event, by student
- Check exists endpoint
- Circuit breaker fallback (mocked Feign)

Run: `cd registration-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- OpenFeign + Resilience4j circuit breaker and retry
- Spring AMQP (RabbitMQ publisher)
