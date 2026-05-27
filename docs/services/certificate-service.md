# Certificate Service

**Port:** 4077
**Database:** `certificate_db`
**Role:** Auto-generate PDF certificates when a student's attendance is confirmed.

---

## Overview

Certificate Service is a RabbitMQ consumer that reacts to `attendance.completed` events. When one arrives, it generates a professionally formatted PDF certificate using Apache PDFBox and stores both the certificate metadata and the raw PDF bytes in the database. Students can then retrieve their certificate metadata and download the PDF via the REST API.

Certificate generation is **idempotent** — re-delivering the same attendance message never creates a duplicate.

---

## Domain Model

### Entity: `Certificate`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `studentId` | String | |
| `studentName` | String | |
| `eventId` | Long | |
| `eventTitle` | String | |
| `registrationId` | Long | **Unique** — one cert per registration |
| `certificateNumber` | String | **Unique** — `CERT-<8-char-UUID>` e.g. `CERT-3F9A2B1C` |
| `issuedAt` | LocalDateTime | |
| `pdfData` | byte[] | Raw PDF bytes stored in `BYTEA` column |

---

## API Endpoints

### Get Certificate Metadata
```
GET /api/certificates/{id}

200 OK:
{
  "id": 1,
  "studentName": "Alice Johnson",
  "eventTitle": "Spring Boot Workshop",
  "certificateNumber": "CERT-3F9A2B1C",
  "issuedAt": "..."
}
404 Not Found
```

### Get by Certificate Number (public verification)
```
GET /api/certificates/number/{certNumber}

200 OK: { ...certificate metadata }
404 Not Found: invalid or unknown certificate number
```

### Get by Registration ID
```
GET /api/certificates/registration/{registrationId}

200 OK: { ...certificate metadata }
404 Not Found
```

### List Student's Certificates
```
GET /api/certificates/student/{studentId}

200 OK: [ { ...certificate metadata }, ... ]
```

### Download Certificate PDF
```
GET /api/certificates/{id}/pdf

200 OK (application/pdf): <binary PDF data>
Content-Disposition: attachment; filename="certificate-CERT-3F9A2B1C.pdf"
404 Not Found
```

---

## RabbitMQ Consumer

**Queue:** `campus.certificate.queue`
**Exchange:** `campus.events`
**Routing key:** `attendance.completed`

On receiving a message:
1. Check if certificate already exists for `registrationId` (idempotency guard)
2. If not, generate PDF using PDFBox with:
   - Student name (large heading)
   - Event title
   - Issue date
   - Unique certificate number
3. Store certificate record + PDF bytes

---

## PDF Generation

Built with Apache PDFBox 3.0.2.

The certificate layout:
```
┌─────────────────────────────────────────────────┐
│                                                 │
│           Certificate of Participation          │  (heading)
│                                                 │
│         This is to certify that                │
│                                                 │
│              Alice Johnson                      │  (student name, large)
│                                                 │
│   has successfully participated in              │
│                                                 │
│           Spring Boot Workshop                  │  (event title)
│                                                 │
│     Certificate No: CERT-3F9A2B1C              │
│     Issued: February 20, 2024                  │
│                                                 │
└─────────────────────────────────────────────────┘
```

Font: `HELVETICA_BOLD` (Standard14Fonts API from PDFBox 3.x).

---

## Certificate Number Format

```
CERT-<8 uppercase hex chars>
e.g. CERT-3F9A2B1C
```

Generated as `"CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`.

---

## Configuration

```yaml
server:
  port: 4077
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/certificate_db}
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
| `DB_URL` | `jdbc:postgresql://localhost:5432/certificate_db` | JDBC URL |
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
- Generate certificate (success)
- Idempotency — second generation for same registrationId returns existing certificate
- Get by ID, by certificate number, by registration ID, by student
- PDF download (Content-Type: application/pdf, non-empty)
- 404 for unknown ID/number

Run: `cd certificate-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- Apache PDFBox 3.0.2 (PDF generation)
- Spring AMQP (RabbitMQ consumer)
- No Feign clients
