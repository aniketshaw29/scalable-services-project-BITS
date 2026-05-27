# Event Service

**Port:** 4071
**Database:** `event_db`
**Role:** Create and manage campus events. Source of truth for event data.

---

## Overview

The Event Service owns all event data. It is one of the foundational services — Registration Service and other services call it via Feign to validate events exist and check capacity. Events move through a status lifecycle from `UPCOMING` to `COMPLETED`.

---

## Domain Model

### Entity: `Event`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated primary key |
| `title` | String | Required |
| `description` | String | |
| `eventDate` | LocalDateTime | Required |
| `endDate` | LocalDateTime | |
| `category` | String | e.g. WORKSHOP, HACKATHON, SEMINAR |
| `maxCapacity` | Integer | Default 100 |
| `currentRegistrations` | Integer | Incremented by Registration Service |
| `status` | Enum | `UPCOMING`, `ONGOING`, `COMPLETED`, `CANCELLED` |
| `venueId` | Long | FK reference to Venue Service (no DB join) |
| `createdAt` | LocalDateTime | |
| `updatedAt` | LocalDateTime | |

---

## API Endpoints

All endpoints are accessible through the gateway at `http://localhost:4069/api/events`.

### Create Event
```
POST /api/events
Content-Type: application/json

{
  "title": "Spring Boot Workshop",
  "description": "Hands-on microservices workshop",
  "eventDate": "2024-02-15T10:00:00",
  "endDate": "2024-02-15T17:00:00",
  "category": "WORKSHOP",
  "maxCapacity": 50
}

201 Created: { "id": 1, "title": "...", "status": "UPCOMING", "currentRegistrations": 0 }
400 Bad Request: validation errors
```

### Get All Events
```
GET /api/events

200 OK: [ { ...event }, ... ]
```

### Get Event by ID
```
GET /api/events/{id}

200 OK: { ...event }
404 Not Found
```

### Get Events by Status
```
GET /api/events/status/{status}
Status values: UPCOMING | ONGOING | COMPLETED | CANCELLED

200 OK: [ { ...event }, ... ]
```

### Update Event
```
PUT /api/events/{id}
Content-Type: application/json

{ ...fields to update }

200 OK: { ...updated event }
404 Not Found
```

### Delete Event
```
DELETE /api/events/{id}

204 No Content
404 Not Found
```

### Update Capacity (internal — called by Registration Service)
```
PUT /api/events/{id}/capacity
Content-Type: application/json

{ "delta": 1 }    // +1 when registering, -1 when cancelling

200 OK: { "currentRegistrations": 5 }
409 Conflict: event is at full capacity
```

---

## Inter-Service Role

Event Service is a **dependency** for other services:

- **Registration Service** calls `GET /api/events/{id}` to validate the event exists and check current capacity before allowing a registration.
- **Venue Service** stores a `venueId` reference but does not call back to Event Service.

Event Service does **not** call any other service (no Feign clients, no RabbitMQ).

---

## Configuration

```yaml
server:
  port: 4071
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/event_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/event_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | Eureka endpoint |

---

## Tests

12 automated integration tests covering:
- Create event (happy path, validation errors)
- Get by ID and by status
- Update event fields
- Delete event
- Capacity increment / decrement
- Capacity conflict (409 when at max)

Run: `cd event-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- No RabbitMQ, no Feign clients (it is called by others, not the caller)
