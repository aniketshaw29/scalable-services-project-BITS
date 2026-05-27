# Leaderboard Service

**Port:** 4079
**Database:** `leaderboard_db`
**Role:** Record and rank competition results. Publish results to trigger notifications.

---

## Overview

Leaderboard Service allows event organisers to publish competition results. It auto-assigns points based on position (1st = 100, 2nd = 75, 3rd = 50, participant = 10), stores results per student per event, and publishes a `results.published` message to RabbitMQ so Notification Service can inform participants.

The `GET /top` endpoint aggregates total points across all events to rank the overall top performers on the platform.

---

## Domain Model

### Enum: `Position`

| Value | Points |
|-------|--------|
| `FIRST` | 100 |
| `SECOND` | 75 |
| `THIRD` | 50 |
| `PARTICIPANT` | 10 |

Points can be overridden per result entry.

### Entity: `Result`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `eventId` | Long | |
| `eventTitle` | String | Denormalized for display |
| `studentId` | String | |
| `studentName` | String | |
| `position` | Enum | `FIRST`, `SECOND`, `THIRD`, `PARTICIPANT` |
| `category` | String | e.g. "Overall", "Frontend Track" |
| `points` | Integer | Auto-assigned from position, or overridden |
| `publishedAt` | LocalDateTime | |

---

## API Endpoints

### Publish Results for Event
```
POST /api/leaderboard/results
Content-Type: application/json

{
  "eventId": 1,
  "eventTitle": "Hackathon 2024",
  "results": [
    {
      "studentId": "STU-001",
      "studentName": "Alice Johnson",
      "position": "FIRST",
      "category": "Overall"
    },
    {
      "studentId": "STU-002",
      "studentName": "Bob Smith",
      "position": "SECOND",
      "category": "Overall"
    }
  ]
}

201 Created: { "published": true, "count": 2 }
400 Bad Request: validation errors
```

### Get Result by ID
```
GET /api/leaderboard/results/{id}

200 OK: { ...result }
404 Not Found
```

### Get Rankings for Event
```
GET /api/leaderboard/event/{eventId}

200 OK: [ { ...result sorted by points descending }, ... ]
```

### Student's Achievements
```
GET /api/leaderboard/student/{studentId}

200 OK: [ { ...result }, ... ]
```

### Overall Top Performers
```
GET /api/leaderboard/top?limit=10

200 OK:
[
  { "studentId": "STU-001", "studentName": "Alice Johnson", "totalPoints": 250 },
  { "studentId": "STU-002", "studentName": "Bob Smith",    "totalPoints": 175 }
]
```

Points are summed across all events. Default limit is 10.

---

## Asynchronous Publishing (RabbitMQ)

After saving results, publishes to exchange `campus.events` with routing key `results.published`:

```json
{
  "eventId": 1,
  "eventTitle": "Hackathon 2024",
  "publishedAt": "2024-02-20T18:00:00",
  "topResults": [
    { "position": 1, "studentId": "STU-001", "studentName": "Alice Johnson", "points": 100 }
  ]
}
```

**Consumer:** Notification Service → logs results notification.

---

## Configuration

```yaml
server:
  port: 4079
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/leaderboard_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/leaderboard_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `RABBITMQ_HOST` | `localhost` | |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `guest` | |
| `RABBITMQ_PASSWORD` | `guest` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

11 automated integration tests covering:
- Publish results (success, multiple students)
- Auto-point assignment per position
- Get result by ID (success and 404)
- Get rankings for event (sorted order)
- Student achievements
- Top performers aggregation
- Validation errors (400)

Run: `cd leaderboard-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- Spring AMQP (RabbitMQ publisher)
- No Feign clients
