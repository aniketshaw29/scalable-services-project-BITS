# Architecture Reference

Detailed technical decisions, data models, and inter-service communication patterns.

---

## Principles

| Principle | How It's Applied |
|-----------|-----------------|
| Single Responsibility | Each service owns exactly one domain |
| Decentralized Data | Each service has its own database — no shared schemas |
| Loose Coupling | Async communication preferred; sync only when immediate response is needed |
| Fail Fast | Circuit breakers prevent cascading failures |
| Stateless Services | No in-memory state — all state in DB or messages |

---

## Service Boundaries and Data Ownership

```
┌─────────────────────────────────────────────────────────────────┐
│ event_db         │ Events, event metadata, capacity             │
├─────────────────────────────────────────────────────────────────┤
│ venue_db         │ Venues, bookings, availability calendar      │
├─────────────────────────────────────────────────────────────────┤
│ registration_db  │ Student registrations per event              │
├─────────────────────────────────────────────────────────────────┤
│ attendance_db    │ Attendance records per registration           │
├─────────────────────────────────────────────────────────────────┤
│ ticket_db        │ QR pass data per registration                │
├─────────────────────────────────────────────────────────────────┤
│ notification_db  │ Notification logs                            │
├─────────────────────────────────────────────────────────────────┤
│ certificate_db   │ Certificate records + PDF blobs              │
├─────────────────────────────────────────────────────────────────┤
│ feedback_db      │ Ratings and comments per event               │
├─────────────────────────────────────────────────────────────────┤
│ leaderboard_db   │ Competition results and rankings             │
├─────────────────────────────────────────────────────────────────┤
│ announcement_db  │ Announcements and broadcasts                 │
├─────────────────────────────────────────────────────────────────┤
│ resource_db      │ File metadata (actual files on disk/S3)      │
├─────────────────────────────────────────────────────────────────┤
│ sponsor_db       │ Sponsors and event-sponsor relationships      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Models

### Event Service

```sql
CREATE TABLE events (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    event_date      TIMESTAMP NOT NULL,
    end_date        TIMESTAMP,
    category        VARCHAR(100),
    max_capacity    INT NOT NULL DEFAULT 100,
    current_registrations INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    venue_id        BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

**Status values:** `UPCOMING` | `ONGOING` | `COMPLETED` | `CANCELLED`

---

### Venue Service

```sql
CREATE TABLE venues (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(255),
    capacity    INT NOT NULL,
    type        VARCHAR(50),     -- AUDITORIUM/CLASSROOM/LAB/OUTDOOR
    facilities  TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE venue_bookings (
    id          BIGSERIAL PRIMARY KEY,
    venue_id    BIGINT NOT NULL REFERENCES venues(id),
    event_id    BIGINT NOT NULL,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL,
    status      VARCHAR(20) DEFAULT 'BOOKED',
    created_at  TIMESTAMP DEFAULT NOW()
);
```

**Conflict detection:** Query overlapping bookings before inserting.

---

### Registration Service

```sql
CREATE TABLE registrations (
    id              BIGSERIAL PRIMARY KEY,
    student_id      VARCHAR(100) NOT NULL,
    student_name    VARCHAR(255) NOT NULL,
    student_email   VARCHAR(255) NOT NULL,
    event_id        BIGINT NOT NULL,
    registered_at   TIMESTAMP DEFAULT NOW(),
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    UNIQUE (student_id, event_id)
);
```

---

### Attendance Service

```sql
CREATE TABLE attendance (
    id                  BIGSERIAL PRIMARY KEY,
    registration_id     BIGINT NOT NULL UNIQUE,
    student_id          VARCHAR(100) NOT NULL,
    event_id            BIGINT NOT NULL,
    marked_at           TIMESTAMP DEFAULT NOW(),
    status              VARCHAR(20) DEFAULT 'PRESENT'
);
```

---

### Ticket Service

```sql
CREATE TABLE tickets (
    id                  BIGSERIAL PRIMARY KEY,
    registration_id     BIGINT NOT NULL UNIQUE,
    student_id          VARCHAR(100) NOT NULL,
    event_id            BIGINT NOT NULL,
    qr_code             TEXT,       -- base64 PNG
    generated_at        TIMESTAMP DEFAULT NOW(),
    status              VARCHAR(20) DEFAULT 'VALID'
);
```

---

### Notification Service

```sql
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    recipient_id    VARCHAR(100),
    recipient_email VARCHAR(255),
    type            VARCHAR(50),    -- REGISTRATION/REMINDER/ANNOUNCEMENT/RESULT
    subject         VARCHAR(255),
    message         TEXT,
    sent_at         TIMESTAMP DEFAULT NOW(),
    status          VARCHAR(20) DEFAULT 'SENT'
);
```

---

### Certificate Service

```sql
CREATE TABLE certificates (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          VARCHAR(100) NOT NULL,
    student_name        VARCHAR(255) NOT NULL,
    event_id            BIGINT NOT NULL,
    event_title         VARCHAR(255) NOT NULL,
    certificate_number  VARCHAR(50) UNIQUE NOT NULL,
    issued_at           TIMESTAMP DEFAULT NOW(),
    pdf_data            BYTEA
);
```

---

### Feedback Service

```sql
CREATE TABLE feedback (
    id              BIGSERIAL PRIMARY KEY,
    student_id      VARCHAR(100) NOT NULL,
    event_id        BIGINT NOT NULL,
    rating          INT CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    submitted_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE (student_id, event_id)
);
```

---

### Leaderboard Service

```sql
CREATE TABLE results (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT NOT NULL,
    event_title     VARCHAR(255),
    student_id      VARCHAR(100) NOT NULL,
    student_name    VARCHAR(255) NOT NULL,
    position        INT,
    category        VARCHAR(100),
    points          INT DEFAULT 0,
    published_at    TIMESTAMP DEFAULT NOW()
);
```

---

### Announcement Service

```sql
CREATE TABLE announcements (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    event_id        BIGINT,
    type            VARCHAR(50),    -- GENERAL/EVENT_UPDATE/VENUE_CHANGE/EMERGENCY
    published_at    TIMESTAMP DEFAULT NOW(),
    published_by    VARCHAR(100)
);
```

---

### Resource Service

```sql
CREATE TABLE resources (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT,
    uploaded_by     VARCHAR(100),
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT,
    storage_key     VARCHAR(500) NOT NULL,
    uploaded_at     TIMESTAMP DEFAULT NOW(),
    description     VARCHAR(500)
);
```

---

### Sponsor Service

```sql
CREATE TABLE sponsors (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    logo_url        VARCHAR(500),
    website         VARCHAR(255),
    tier            VARCHAR(20),    -- PLATINUM/GOLD/SILVER/BRONZE
    contact_person  VARCHAR(255),
    contact_email   VARCHAR(255),
    description     TEXT
);

CREATE TABLE event_sponsors (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT NOT NULL,
    sponsor_id      BIGINT NOT NULL REFERENCES sponsors(id),
    contribution    DECIMAL(12,2),
    notes           TEXT,
    UNIQUE (event_id, sponsor_id)
);
```

---

## Synchronous Communication

### Feign Client Pattern

Every Feign client has a corresponding circuit breaker fallback:

```java
// In Registration Service
@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {
    @GetMapping("/api/events/{id}")
    EventResponse getEvent(@PathVariable Long id);
}

@Component
public class EventClientFallback implements EventClient {
    @Override
    public EventResponse getEvent(Long id) {
        throw new ServiceUnavailableException("Event service is currently unavailable");
    }
}
```

### Resilience4j Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      event-service:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      event-service:
        maxAttempts: 3
        waitDuration: 500ms
```

---

## Asynchronous Communication

### RabbitMQ Topology

```
Exchange: campus.events (type: topic, durable: true)

Bindings:
  campus.ticket.queue        ← registration.completed
  campus.notification.queue  ← registration.completed
  campus.notification.queue  ← announcement.created
  campus.notification.queue  ← results.published
  campus.certificate.queue   ← attendance.completed
  campus.notification.queue  ← attendance.completed

Dead Letter Exchange: campus.dlx
Dead Letter Queue:    campus.dead-letters
```

### Message Envelope (all events)

```json
{
  "eventType": "registration.completed",
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": { ... event-specific data ... }
}
```

---

## API Gateway Routes

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: event-service
          uri: lb://event-service
          predicates: [Path=/api/events/**]

        - id: registration-service
          uri: lb://registration-service
          predicates: [Path=/api/registrations/**]

        - id: venue-service
          uri: lb://venue-service
          predicates: [Path=/api/venues/**]

        - id: attendance-service
          uri: lb://attendance-service
          predicates: [Path=/api/attendance/**]

        - id: ticket-service
          uri: lb://ticket-service
          predicates: [Path=/api/tickets/**]

        - id: notification-service
          uri: lb://notification-service
          predicates: [Path=/api/notifications/**]

        - id: certificate-service
          uri: lb://certificate-service
          predicates: [Path=/api/certificates/**]

        - id: feedback-service
          uri: lb://feedback-service
          predicates: [Path=/api/feedback/**]

        - id: leaderboard-service
          uri: lb://leaderboard-service
          predicates: [Path=/api/leaderboard/**]

        - id: announcement-service
          uri: lb://announcement-service
          predicates: [Path=/api/announcements/**]

        - id: resource-service
          uri: lb://resource-service
          predicates: [Path=/api/resources/**]

        - id: sponsor-service
          uri: lb://sponsor-service
          predicates: [Path=/api/sponsors/**]
```

---

## Port Reference

| Service | Port |
|---------|------|
| API Gateway | 4069 |
| Eureka Server | 4070 |
| Event Service | 4071 |
| Registration Service | 4072 |
| Venue Service | 4073 |
| Attendance Service | 4074 |
| Ticket Service | 4075 |
| Notification Service | 4076 |
| Certificate Service | 4077 |
| Feedback Service | 4078 |
| Leaderboard Service | 4079 |
| Announcement Service | 4080 |
| Resource Service | 4081 |
| Sponsor Service | 4082 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Management | 15672 |
| PostgreSQL (per service) | 5480–5491 |

<!-- reviewed -->
