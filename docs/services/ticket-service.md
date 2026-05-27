# Ticket Service

**Port:** 4075
**Database:** `ticket_db`
**Role:** Generate QR code passes on registration and validate tickets at event entry.

---

## Overview

Ticket Service is a RabbitMQ consumer. It does not expose a registration endpoint — QR tickets are generated automatically when a `registration.completed` message arrives from Registration Service. Each ticket contains a ZXing-generated QR code (base64 PNG) encoding the registration, student, and event IDs for scanner validation.

Ticket generation is **idempotent** — if the message is re-delivered, no duplicate ticket is created.

---

## Domain Model

### Entity: `Ticket`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `registrationId` | Long | **Unique** — one ticket per registration |
| `studentId` | String | |
| `eventId` | Long | |
| `qrCode` | String (Text) | `data:image/png;base64,…` — base64 encoded PNG |
| `generatedAt` | LocalDateTime | |
| `status` | Enum | `VALID`, `USED`, `CANCELLED` |

---

## API Endpoints

### Get Ticket for Registration
```
GET /api/tickets/registration/{registrationId}

200 OK:
{
  "id": 1,
  "registrationId": 1,
  "studentId": "STU-001",
  "eventId": 1,
  "qrCode": "data:image/png;base64,iVBORw0KGgo...",
  "status": "VALID",
  "generatedAt": "..."
}
404 Not Found: ticket not yet generated (registration message not yet processed)
```

### Validate Ticket at Entry
```
GET /api/tickets/{id}/validate

200 OK: { "valid": true, "ticketId": 1, "studentId": "STU-001", "eventId": 1 }
200 OK: { "valid": false, "reason": "ALREADY_USED" }
200 OK: { "valid": false, "reason": "CANCELLED" }
404 Not Found
```

### Mark Ticket as Used
```
PUT /api/tickets/{id}/mark-used

200 OK: { "id": 1, "status": "USED" }
404 Not Found
```

---

## RabbitMQ Consumer

**Queue:** `campus.ticket.queue`
**Exchange:** `campus.events`
**Routing key:** `registration.completed`

On receiving a message:
1. Check if a ticket already exists for `registrationId` (idempotency)
2. If not, validate the registration via Feign (`GET /api/registrations/{id}/exists`)
3. Generate QR code using ZXing containing:
   ```json
   { "ticketId": "<uuid>", "registrationId": 1, "studentId": "STU-001", "eventId": 1 }
   ```
4. Store ticket with `status: VALID`

---

## Synchronous Dependency (Feign)

### RegistrationClient → Registration Service

Called before QR generation to confirm the registration is still `ACTIVE`.

**Fallback:** Logs error, skips ticket generation (message will go to DLX after retries exhaust).

---

## QR Code Format

QR codes are generated using ZXing (Zebra Crossing) at 300×300 pixels and stored as base64 PNG:

```
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...
```

This string can be embedded directly in an HTML `<img>` tag for display.

---

## Configuration

```yaml
server:
  port: 4075
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/ticket_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/ticket_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `RABBITMQ_HOST` | `localhost` | |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `guest` | |
| `RABBITMQ_PASSWORD` | `guest` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

7 automated integration tests covering:
- QR generation via service call (simulates consumer)
- Idempotency — second call for same registrationId returns existing ticket
- Get ticket by registration ID
- Validate ticket (valid / already used)
- Mark ticket as used
- 404 for unknown ticket

Run: `cd ticket-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- ZXing Core + Java SE (`com.google.zxing`) for QR generation
- Spring AMQP (RabbitMQ consumer)
- OpenFeign (validates registration before generating)
