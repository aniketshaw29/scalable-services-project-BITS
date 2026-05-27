# Feedback Service

**Port:** 4078
**Database:** `feedback_db`
**Role:** Collect event feedback from students and compute aggregated summaries.

---

## Overview

Feedback Service lets students rate events (1–5 stars) and leave comments after attending. Each student can submit feedback for an event exactly once — duplicates are rejected with 409. The service computes real-time aggregated summaries (average rating and distribution) on demand without storing them separately.

---

## Domain Model

### Entity: `Feedback`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `studentId` | String | Required |
| `studentName` | String | |
| `eventId` | Long | Required |
| `rating` | Integer | 1–5, required |
| `comment` | String | Max 1000 chars, optional |
| `submittedAt` | LocalDateTime | |

**Unique constraint:** `(student_id, event_id)` — one feedback per student per event.

---

## API Endpoints

### Submit Feedback
```
POST /api/feedback
Content-Type: application/json

{
  "studentId": "STU-001",
  "studentName": "Alice Johnson",
  "eventId": 1,
  "rating": 5,
  "comment": "Excellent workshop, very hands-on!"
}

201 Created: { "id": 1, "rating": 5, "submittedAt": "..." }
400 Bad Request: rating not between 1 and 5
409 Conflict: feedback already submitted for this event
```

### Get Feedback by ID
```
GET /api/feedback/{id}

200 OK: { ...feedback }
404 Not Found
```

### Get All Feedback for Event
```
GET /api/feedback/event/{eventId}

200 OK: [ { ...feedback }, ... ]
```

### Get Aggregated Summary for Event
```
GET /api/feedback/event/{eventId}/summary

200 OK:
{
  "eventId": 1,
  "averageRating": 4.6,
  "totalResponses": 35,
  "ratingDistribution": {
    "1": 1,
    "2": 1,
    "3": 3,
    "4": 10,
    "5": 20
  }
}
```

Average is rounded to 1 decimal place. Distribution is computed in-memory via Java streams from `findByEventId()`.

### Student's Feedback History
```
GET /api/feedback/student/{studentId}

200 OK: [ { ...feedback }, ... ]
```

---

## No Async Dependencies

Feedback Service is self-contained:
- No Feign clients (does not call other services)
- No RabbitMQ (does not publish or consume)

---

## Configuration

```yaml
server:
  port: 4078
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/feedback_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:4070/eureka/}
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/feedback_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

12 automated integration tests covering:
- Submit feedback (success)
- Duplicate submission (409)
- Rating out of range (400)
- Get by ID (success and 404)
- Get all for event
- Aggregated summary (correct average, distribution counts)
- Student feedback history
- Multiple students, multiple events

Run: `cd feedback-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- No RabbitMQ, no Feign clients
