# Notification Service

**Port:** 4076
**Database:** `notification_db`
**Role:** Log notifications triggered by platform events (registration, announcements, results).

---

## Overview

Notification Service is a pure RabbitMQ consumer. It does not initiate any actions — it listens for messages published by other services and records a notification log entry for each one. In a production system this would send real emails or push notifications; in this implementation it stores a structured log in the database that can be queried per student.

---

## Domain Model

### Entity: `Notification`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `recipientId` | String | Student ID (null for broadcasts) |
| `recipientEmail` | String | |
| `type` | Enum | `REGISTRATION`, `REMINDER`, `VENUE_CHANGE`, `ANNOUNCEMENT`, `RESULT` |
| `subject` | String | Notification title |
| `message` | String (Text) | Full notification body |
| `sentAt` | LocalDateTime | When the notification was logged |
| `status` | Enum | `SENT`, `FAILED` |

---

## API Endpoints

### Get Student's Notifications
```
GET /api/notifications/student/{studentId}

200 OK: [ { ...notification }, ... ]
```

### Get Notification Detail
```
GET /api/notifications/{id}

200 OK:
{
  "id": 1,
  "type": "REGISTRATION",
  "subject": "Registration Confirmed: Spring Boot Workshop",
  "message": "You have successfully registered for Spring Boot Workshop on Feb 20, 2024.",
  "sentAt": "...",
  "status": "SENT"
}
404 Not Found
```

### Filter by Type
```
GET /api/notifications/type/{type}
Type values: REGISTRATION | REMINDER | VENUE_CHANGE | ANNOUNCEMENT | RESULT

200 OK: [ { ...notification }, ... ]
```

---

## RabbitMQ Consumers

**Queue:** `campus.notification.queue`

This single queue is bound to multiple routing keys:

| Routing Key | Source Service | Action |
|-------------|---------------|--------|
| `registration.completed` | Registration Service | Log registration confirmation for student |
| `announcement.created` | Announcement Service | Log broadcast announcement |
| `results.published` | Leaderboard Service | Log results notification |

### Message handling

**`registration.completed`:**
```
type:    REGISTRATION
subject: "Registration Confirmed: <eventTitle>"
message: "You have successfully registered for <eventTitle>."
recipientId:    <studentId>
recipientEmail: <studentEmail>
```

**`announcement.created`:**
```
type:    ANNOUNCEMENT (or VENUE_CHANGE/EMERGENCY based on announcement type)
subject: <announcement title>
message: <announcement content>
recipientId: null (broadcast)
```

**`results.published`:**
```
type:    RESULT
subject: "Results Published: <eventTitle>"
message: "Results for <eventTitle> are now available."
recipientId: null (broadcast)
```

---

## Idempotency Note

Notification Service **allows duplicates** — it is an append-only log. Re-delivered messages result in an additional log entry, which is acceptable for a notification history.

---

## Configuration

```yaml
server:
  port: 4076
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/notification_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/notification_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `RABBITMQ_HOST` | `localhost` | |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `guest` | |
| `RABBITMQ_PASSWORD` | `guest` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

9 automated integration tests covering:
- Save notification directly
- Get by ID (success and 404)
- Get by student ID
- Get by type
- Multiple notifications for same student

Run: `cd notification-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- Spring AMQP (RabbitMQ consumer — 3 routing key bindings)
- No Feign clients (pure consumer)
