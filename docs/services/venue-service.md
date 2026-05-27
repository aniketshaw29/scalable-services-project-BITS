# Venue Service

**Port:** 4073
**Database:** `venue_db`
**Role:** Manage physical venues and prevent double-booking via time-slot conflict detection.

---

## Overview

The Venue Service manages campus spaces (auditoriums, classrooms, labs, outdoor areas) and their booking calendar. Its core feature is **conflict detection** — it rejects bookings that overlap with an existing booking for the same venue.

Registration Service calls Venue Service to confirm a venue is assigned to an event. Venues are booked by specifying an event ID and time range.

---

## Domain Model

### Entity: `Venue`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `name` | String | Required |
| `location` | String | Building/room description |
| `capacity` | Integer | Max people |
| `type` | Enum | `AUDITORIUM`, `CLASSROOM`, `LAB`, `OUTDOOR` |
| `facilities` | String | Comma-separated list (projector, AC, etc.) |
| `createdAt` | LocalDateTime | |

### Entity: `VenueBooking`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | |
| `venueId` | Long | FK to Venue |
| `eventId` | Long | Reference to Event (no DB join) |
| `startTime` | LocalDateTime | |
| `endTime` | LocalDateTime | |
| `status` | Enum | `BOOKED`, `CANCELLED` |
| `createdAt` | LocalDateTime | |

---

## API Endpoints

### Create Venue
```
POST /api/venues
Content-Type: application/json

{
  "name": "Main Auditorium",
  "location": "Block A, Ground Floor",
  "capacity": 500,
  "type": "AUDITORIUM",
  "facilities": "Projector, Sound System, AC"
}

201 Created: { "id": 1, ... }
```

### List All Venues
```
GET /api/venues

200 OK: [ { ...venue }, ... ]
```

### Get Venue by ID
```
GET /api/venues/{id}

200 OK: { ...venue }
404 Not Found
```

### Check Availability
```
GET /api/venues/{id}/availability?startTime=2024-02-15T09:00:00&endTime=2024-02-15T18:00:00

200 OK: { "available": true }
200 OK: { "available": false, "conflict": { ...conflicting booking } }
```

### Book Venue for Event
```
POST /api/venues/{id}/book
Content-Type: application/json

{
  "eventId": 1,
  "startTime": "2024-02-15T09:00:00",
  "endTime":   "2024-02-15T18:00:00"
}

201 Created: { "bookingId": 1, "venueId": 1, "eventId": 1, "status": "BOOKED" }
409 Conflict: venue already booked for this time slot
```

### Cancel Booking
```
DELETE /api/venues/bookings/{bookingId}

204 No Content
404 Not Found
```

### Get Venue for Event (called by Registration Service)
```
GET /api/venues/event/{eventId}

200 OK: { ...venue }
404 Not Found: no venue assigned to this event
```

---

## Conflict Detection Logic

Before creating a booking, the service queries for any overlapping active bookings:

```
Overlap condition:
  existing.startTime < requested.endTime
  AND
  existing.endTime   > requested.startTime
  AND
  existing.status = 'BOOKED'
```

If any overlapping booking is found, the request is rejected with `409 Conflict`.

---

## Inter-Service Role

- **Called by Registration Service** via Feign: `GET /api/venues/event/{eventId}` — confirms a venue is assigned to the event being registered for.
- Venue Service does **not** call any other service.

---

## Configuration

```yaml
server:
  port: 4073
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/venue_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/venue_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | Eureka endpoint |

---

## Tests

13 automated integration tests covering:
- Create, list, get by ID
- Book a venue (success)
- Double-booking conflict (409)
- Check availability (available / not available)
- Cancel booking
- Get venue by event ID

Run: `cd venue-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- No RabbitMQ, no Feign clients
