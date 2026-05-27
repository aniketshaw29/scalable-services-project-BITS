# Announcement Service

**Port:** 4080
**Database:** `announcement_db`
**Role:** Publish announcements and broadcast them to all subscribers via RabbitMQ.

---

## Overview

Announcement Service lets administrators and organisers publish platform-wide or event-specific announcements. Each announcement is typed (GENERAL, EVENT_UPDATE, VENUE_CHANGE, EMERGENCY) to allow subscribers to filter by severity. On creation, a `announcement.created` message is published to RabbitMQ, which Notification Service consumes to log a broadcast notification.

---

## Domain Model

### Enum: `AnnouncementType`

| Value | Use Case |
|-------|----------|
| `GENERAL` | Platform news, general info |
| `EVENT_UPDATE` | Schedule or detail change for an event |
| `VENUE_CHANGE` | Venue or room change notice |
| `EMERGENCY` | Cancellations, urgent alerts |

### Entity: `Announcement`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `title` | String | Required |
| `content` | String | Max 2000 chars, required |
| `eventId` | Long | Optional — null for platform-wide announcements |
| `type` | Enum | Defaults to `GENERAL` if not provided |
| `publishedBy` | String | Author identifier |
| `publishedAt` | LocalDateTime | |

---

## API Endpoints

### Create Announcement
```
POST /api/announcements
Content-Type: application/json

{
  "title": "Venue Change Notice",
  "content": "The venue for Spring Boot Workshop has been moved to Room 301, Block B.",
  "eventId": 1,
  "type": "VENUE_CHANGE",
  "publishedBy": "admin"
}

201 Created: { "id": 1, "publishedAt": "..." }
400 Bad Request: missing title or content
```

Type defaults to `GENERAL` if omitted.

### Get Announcement by ID
```
GET /api/announcements/{id}

200 OK: { ...announcement }
404 Not Found
```

### List All Announcements
```
GET /api/announcements

200 OK: [ { ...announcement }, ... ]   (newest first)
```

### Announcements for a Specific Event
```
GET /api/announcements/event/{eventId}

200 OK: [ { ...announcement }, ... ]
```

### Filter by Type
```
GET /api/announcements/type/{type}
Type values: GENERAL | EVENT_UPDATE | VENUE_CHANGE | EMERGENCY

200 OK: [ { ...announcement }, ... ]
```

---

## Asynchronous Publishing (RabbitMQ)

On every successful announcement creation, publishes to exchange `campus.events` with routing key `announcement.created`:

```json
{
  "announcementId": 1,
  "title": "Venue Change Notice",
  "content": "The venue for Spring Boot Workshop has been changed to Room 301, Block B.",
  "eventId": 1,
  "type": "VENUE_CHANGE",
  "publishedBy": "admin",
  "publishedAt": "2024-02-18T09:00:00"
}
```

**Consumer:** Notification Service → logs broadcast notification.

---

## Configuration

```yaml
server:
  port: 4080
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/announcement_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/announcement_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `RABBITMQ_HOST` | `localhost` | |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `guest` | |
| `RABBITMQ_PASSWORD` | `guest` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

10 automated integration tests covering:
- Create announcement (success)
- Create without type (defaults to GENERAL)
- Validation errors — missing title/content (400)
- Get by ID (success and 404)
- List all announcements
- Filter by event
- Filter by type

Run: `cd announcement-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- Spring AMQP (RabbitMQ publisher)
- No Feign clients
