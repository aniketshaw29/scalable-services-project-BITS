# Attendance Service

**Port:** 4074
**Database:** `attendance_db`
**Role:** Record student attendance at events and trigger certificate generation.

---

## Overview

Attendance Service is the gateway to the certificate pipeline. When an organiser marks a student present, the service validates the registration via a Feign call to Registration Service, records the attendance, and publishes an `attendance.completed` event to RabbitMQ. Certificate Service consumes this message and generates the PDF certificate automatically.

A registration can only be marked once — duplicate attempts return 409.

---

## Domain Model

### Entity: `Attendance`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `registrationId` | Long | **Unique** — one record per registration |
| `studentId` | String | Copied from registration |
| `studentName` | String | Copied from registration |
| `studentEmail` | String | Copied from registration |
| `eventId` | Long | |
| `eventTitle` | String | Stored for denormalized reporting |
| `markedAt` | LocalDateTime | When attendance was recorded |
| `status` | Enum | `PRESENT`, `ABSENT` |

---

## API Endpoints

### Mark Attendance
```
POST /api/attendance
Content-Type: application/json

{
  "registrationId": 1,
  "studentId": "STU-001",
  "studentName": "Alice Johnson",
  "studentEmail": "alice@college.edu",
  "eventId": 1,
  "eventTitle": "Spring Boot Workshop"
}

201 Created: { "id": 1, "status": "PRESENT", "markedAt": "..." }
404 Not Found: registration does not exist
409 Conflict: attendance already marked for this registration
503 Service Unavailable: registration service unavailable (circuit breaker)
```

### Get Attendance Record
```
GET /api/attendance/{id}

200 OK: { ...attendance }
404 Not Found
```

### Check Attendance Status
```
GET /api/attendance/{registrationId}/status

200 OK: { "present": true, "markedAt": "..." }
200 OK: { "present": false }
```

### List Attendees for Event
```
GET /api/attendance/event/{eventId}

200 OK: [ { ...attendance }, ... ]
```

### Student's Attendance History
```
GET /api/attendance/student/{studentId}

200 OK: [ { ...attendance }, ... ]
```

---

## Synchronous Dependency (Feign)

### RegistrationClient → Registration Service

Called at mark-attendance time: `GET /api/registrations/{id}/exists`

- If `exists: false` → returns 404
- If `exists: true` → proceeds with recording attendance

**Fallback:** Throws `RegistrationServiceUnavailableException` (503).

---

## Asynchronous Publishing (RabbitMQ)

On successful attendance marking, publishes to exchange `campus.events` with routing key `attendance.completed`:

```json
{
  "attendanceId": 1,
  "registrationId": 1,
  "studentId": "STU-001",
  "studentName": "Alice Johnson",
  "studentEmail": "alice@college.edu",
  "eventId": 1,
  "eventTitle": "Spring Boot Workshop",
  "markedAt": "2024-02-20T10:45:00"
}
```

**Consumer:** Certificate Service → generates PDF certificate.

---

## Configuration

```yaml
server:
  port: 4074
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/attendance_db}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:4070/eureka/}
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/attendance_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `RABBITMQ_HOST` | `localhost` | |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `guest` | |
| `RABBITMQ_PASSWORD` | `guest` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

12 automated integration tests covering:
- Mark attendance (success)
- Duplicate registration (409)
- Registration not found (404)
- Validation errors (400)
- Get by ID
- Status check (present / not present)
- List by event and by student

Run: `cd attendance-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- OpenFeign + Resilience4j
- Spring AMQP (RabbitMQ publisher)
