# Sponsor Service

**Port:** 4082
**Database:** `sponsor_db`
**Role:** Manage sponsor profiles and link sponsors to events with contribution details.

---

## Overview

Sponsor Service maintains a catalogue of corporate sponsors (with name, logo, website, tier, and contact details) and tracks which sponsors are supporting which events, including contribution amounts and notes. A sponsor can be linked to multiple events; an event can have multiple sponsors — but the same sponsor cannot be linked to the same event twice (409 on duplicate).

---

## Domain Model

### Enum: `SponsorTier`

| Value | Typical Use |
|-------|-------------|
| `PLATINUM` | Title sponsor, highest contribution |
| `GOLD` | Major sponsor |
| `SILVER` | Supporting sponsor |
| `BRONZE` | Basic sponsor — default tier |

### Entity: `Sponsor`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `name` | String | Required |
| `logoUrl` | String | Optional URL |
| `website` | String | Optional URL |
| `tier` | Enum | Defaults to `BRONZE` |
| `contactPerson` | String | |
| `contactEmail` | String | |
| `description` | String | |

### Entity: `EventSponsor`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `eventId` | Long | Event being sponsored |
| `sponsor` | Sponsor | `@ManyToOne` FK |
| `contribution` | BigDecimal | Optional monetary contribution |
| `notes` | String | Optional notes |
| `linkedAt` | LocalDateTime | |

**Unique constraint:** `(event_id, sponsor_id)` — one link per sponsor per event.

---

## API Endpoints

### Create Sponsor
```
POST /api/sponsors
Content-Type: application/json

{
  "name": "TechCorp",
  "logoUrl": "https://techcorp.com/logo.png",
  "website": "https://techcorp.com",
  "tier": "GOLD",
  "contactPerson": "John Doe",
  "contactEmail": "john@techcorp.com",
  "description": "Leading technology solutions company"
}

201 Created: { "id": 1, "name": "TechCorp", "tier": "GOLD", ... }
400 Bad Request: name is required
```

### List All Sponsors
```
GET /api/sponsors

200 OK: [ { ...sponsor }, ... ]
```

### Get Sponsor by ID
```
GET /api/sponsors/{id}

200 OK: { ...sponsor }
404 Not Found
```

### Update Sponsor
```
PUT /api/sponsors/{id}
Content-Type: application/json

{ ...fields to update }

200 OK: { ...updated sponsor }
404 Not Found
```

### Link Sponsor to Event
```
POST /api/sponsors/{sponsorId}/events/{eventId}
Content-Type: application/json

{
  "contribution": 50000.00,
  "notes": "Primary sponsor for refreshments and prizes"
}

201 Created: { "id": 1, "eventId": 1, "sponsor": { ...sponsor }, "contribution": 50000.00 }
404 Not Found: sponsor not found
409 Conflict: sponsor already linked to this event
```

### Get Sponsors for Event
```
GET /api/sponsors/event/{eventId}

200 OK:
[
  {
    "id": 1,
    "eventId": 1,
    "sponsor": { "id": 1, "name": "TechCorp", "tier": "GOLD", ... },
    "contribution": 50000.00,
    "notes": "...",
    "linkedAt": "..."
  }
]
```

---

## No Async Dependencies

Sponsor Service is self-contained:
- No Feign clients
- No RabbitMQ

---

## Configuration

```yaml
server:
  port: 4082
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/sponsor_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/sponsor_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

---

## Tests

12 automated integration tests covering:
- Create sponsor (success)
- Default tier (BRONZE when not specified)
- Validation error (400 on missing name)
- Get by ID (success and 404)
- List all sponsors
- Update sponsor
- Link sponsor to event (success)
- Duplicate link (409)
- Get sponsors for event
- 404 for unknown sponsor on link

Run: `cd sponsor-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- No RabbitMQ, no Feign clients
