# Campus EventHub — Phased Development Plan

Each phase is self-contained with a clear scope, implementation checklist, and test criteria.
**Only move to the next phase after all tests in the current phase pass.**

---

## Phase Overview

| Phase | Status | Focus | Services Built | Key Milestone |
|-------|--------|-------|----------------|---------------|
| 1 | ✅ DONE | Infrastructure Foundation | Eureka, API Gateway | Services discover each other |
| 2 | ✅ DONE | Core Event Domain | Event Service, Venue Service | Events and venues CRUD working |
| 3 | ✅ DONE | Registration + Resilience | Registration Service | Circuit breaker demo working |
| 4 | ✅ DONE | Async Messaging | Ticket, Notification | RabbitMQ flow working end-to-end |
| 5 | ✅ DONE | Attendance + Certificate | Attendance, Certificate | Full student lifecycle complete |
| 6 | ✅ DONE | Engagement Layer | Feedback, Leaderboard, Announcement | Event engagement features complete |
| 7 | ✅ DONE | Utility Services | Resource Upload, Sponsor | Supporting features complete |
| 8 | 🔲 TODO | Containerization | All services | docker-compose up brings everything up |
| 9 | 🔲 TODO | Kubernetes | All services | kubectl apply deploys full system |
| 10 | 🔲 TODO | Frontend Dashboard | React/Thymeleaf | End-to-end demo via UI |

---

## Phase 1 — Infrastructure Foundation ✅

**Goal:** Eureka Server + API Gateway running. All future services will register here.

### What to Build

#### 1.1 Maven Parent POM (`pom.xml`)
- Java 17, Spring Boot 3.2.x parent
- Spring Cloud 2023.x BOM
- Common dependency management (Eureka client, Feign, Resilience4j, RabbitMQ)
- All service modules listed

#### 1.2 Eureka Server (`eureka-server/`)
```
eureka-server/
├── pom.xml
└── src/main/
    ├── java/.../EurekaServerApplication.java
    └── resources/application.yml
```
- `@EnableEurekaServer`
- Runs on port `4070`
- Standalone mode (does not register itself)

#### 1.3 API Gateway (`api-gateway/`)
```
api-gateway/
├── pom.xml
└── src/main/
    ├── java/.../ApiGatewayApplication.java
    └── resources/application.yml
```
- Spring Cloud Gateway + Eureka client
- Runs on port `4069`
- Route rules: `/api/events/**` → `event-service`, etc.
- Global CORS filter

### Test Criteria
- [x] `mvn clean package` succeeds for both modules
- [x] Eureka Server context loads and actuator/health returns UP (automated test)
- [x] API Gateway context loads and actuator/health returns UP (automated test)
- [ ] Eureka UI at `http://localhost:4070` loads (requires running stack)
- [ ] `http://localhost:4070` shows `API-GATEWAY` in registered instances (requires running stack)

---

## Phase 2 — Core Event Domain ✅

**Goal:** Event and Venue services fully functional with their own databases.

### What to Build

#### 2.1 Event Service (`event-service/`)

**Entity: `Event`**
```
id, title, description, date, endDate, category,
maxCapacity, currentRegistrations, status (UPCOMING/ONGOING/COMPLETED),
venueId, createdAt, updatedAt
```

**Endpoints:**
```
POST   /api/events               → Create event
GET    /api/events               → List all events
GET    /api/events/{id}          → Get event by ID
GET    /api/events/status/{status} → Filter by status
PUT    /api/events/{id}          → Update event
DELETE /api/events/{id}          → Delete event
PUT    /api/events/{id}/capacity → Increment/decrement registration count
```

**Database:** `event_db` (PostgreSQL)

#### 2.2 Venue Service (`venue-service/`)

**Entity: `Venue`**
```
id, name, location, capacity, type (AUDITORIUM/CLASSROOM/LAB/OUTDOOR),
facilities, createdAt
```

**Entity: `VenueBooking`**
```
id, venueId, eventId, startTime, endTime, status (BOOKED/CANCELLED)
```

**Endpoints:**
```
POST   /api/venues                    → Create venue
GET    /api/venues                    → List all venues
GET    /api/venues/{id}               → Get venue by ID
GET    /api/venues/{id}/availability  → Check availability for time range
POST   /api/venues/{id}/book          → Book venue for event
DELETE /api/venues/bookings/{bookingId} → Cancel booking
GET    /api/venues/event/{eventId}    → Get venue for an event
```

**Database:** `venue_db`

### Test Criteria
- [x] event-service: 12/12 automated tests pass (CRUD, capacity limits, conflict handling)
- [x] venue-service: 13/13 automated tests pass (CRUD, booking, conflict detection, cancel)
- [x] Double-booking conflict detection rejects overlapping time ranges (automated)
- [x] Capacity over-max returns 409 (automated)
- [ ] Both services register in Eureka (requires running stack)
- [ ] Create event via Gateway: `POST http://localhost:4069/api/events` (requires running stack)
- [ ] PostgreSQL DBs `event_db` and `venue_db` have correct tables (requires running stack)

---

## Phase 3 — Registration Service + Resilience ✅

**Goal:** Students can register for events. Circuit breaker demo works when Event Service is down.

### What to Build

#### 3.1 Registration Service (`registration-service/`)

**Entity: `Registration`**
```
id, studentId, studentName, studentEmail, eventId,
registeredAt, status (ACTIVE/CANCELLED)
```

**Sync calls (Feign clients):**
- `EventClient` → GET `/api/events/{id}` — validate event exists and get capacity
- `VenueClient` → GET `/api/venues/event/{eventId}` — confirm venue is assigned

**Circuit Breaker (Resilience4j):**
- Fallback when Event Service is unreachable: return `503` with message
  `"Event service unavailable. Please try again later."`
- Retry: 3 attempts with 500ms wait
- Circuit opens after 50% failure rate over 10 calls

**Endpoints:**
```
POST   /api/registrations              → Register student for event
GET    /api/registrations/{id}         → Get registration by ID
GET    /api/registrations/event/{eventId} → List registrations for event
GET    /api/registrations/student/{studentId} → Student's registrations
DELETE /api/registrations/{id}         → Cancel registration
GET    /api/registrations/{id}/exists  → Check if registration exists (used by other services)
```

**RabbitMQ Publisher:** (wired in Phase 4)
- Exchange: `campus.events`
- Routing key: `registration.completed`
- Payload: `{ registrationId, studentId, studentEmail, eventId, eventTitle, timestamp }`

**Database:** `registration_db`

### Test Criteria
- [x] registration-service: 14/14 automated tests pass
- [x] Register student validates event capacity via Feign (mocked in tests)
- [x] Duplicate registration returns 409 (automated)
- [x] Event at capacity returns 409 (automated)
- [x] Cancel registration sets status CANCELLED (automated)
- [x] `GET /api/registrations/{id}/exists` returns correct exists/status (automated)
- [ ] Registration publishes `registration.completed` to RabbitMQ (Phase 4)
- [ ] **Circuit breaker test:** Stop Event Service → Registration returns fallback 503 (requires running stack)
- [ ] Eureka shows all 3 services (event, venue, registration) registered (requires running stack)

---

## Phase 4 — Async Messaging (Ticket + Notification) ✅

**Goal:** After registration, QR pass is generated and notification is sent automatically via RabbitMQ.

### What to Build

#### 4.1 RabbitMQ Configuration
Establish shared exchange/queue topology (configured in each service):
```
Exchange: campus.events (topic exchange)

Queues:
  campus.ticket.queue       ← routing key: registration.completed
  campus.notification.queue ← routing keys: registration.completed,
                                             announcement.created,
                                             results.published,
                                             attendance.completed (for cert notification)
  campus.certificate.queue  ← routing key: attendance.completed
```

#### 4.2 Ticket Service (`ticket-service/`)

**Entity: `Ticket`**
```
id, registrationId, studentId, eventId, qrCode (base64 PNG),
generatedAt, status (VALID/USED/CANCELLED)
```

**Sync call (Feign):**
- `RegistrationClient` → validate registration exists before issuing QR

**RabbitMQ Consumer:**
- Queue: `campus.ticket.queue`
- On receive: call ZXing to generate QR code containing JSON payload
  `{ ticketId, registrationId, studentId, eventId }`, store as base64

**Endpoints:**
```
GET  /api/tickets/registration/{registrationId} → Get ticket with QR
GET  /api/tickets/{id}/validate                 → Validate ticket (used at entry)
PUT  /api/tickets/{id}/mark-used                → Mark ticket as used
```

**Database:** `ticket_db`

#### 4.3 Notification Service (`notification-service/`)

**Entity: `Notification`**
```
id, recipientId, recipientEmail, type (REGISTRATION/REMINDER/VENUE_CHANGE/ANNOUNCEMENT/RESULT),
subject, message, sentAt, status (SENT/FAILED)
```

**RabbitMQ Consumer:**
- Queue: `campus.notification.queue`
- Handles: `registration.completed`, `announcement.created`, `results.published`
- Logs notification to DB (simulated send — log to console or store in DB)

**Endpoints:**
```
GET  /api/notifications/student/{studentId}   → Get student's notifications
GET  /api/notifications/{id}                  → Get notification detail
GET  /api/notifications/type/{type}           → Filter by type
```

**Database:** `notification_db`

### Test Criteria
- [x] ticket-service: 7/7 automated tests pass (QR generation, idempotency, get, validate, mark-used)
- [x] notification-service: 9/9 automated tests pass (save, get by id/student/type, 404 handling)
- [x] QR code is base64 PNG starting with `data:image/png;base64,` (automated)
- [x] Ticket generation is idempotent — same registrationId never creates a duplicate (automated)
- [x] registration-service: 14/14 tests still pass after adding RabbitMQ publisher (automated)
- [ ] Register a student → `registration.completed` message visible in RabbitMQ UI (requires running stack)
- [ ] Ticket row created in `ticket_db` with non-null `qrCode` field (requires running stack)
- [ ] Notification row created in `notification_db` (requires running stack)
- [ ] RabbitMQ dead-letter queue configured — message not lost if consumer crashes (requires running stack)

---

## Phase 5 — Attendance + Certificate ✅

**Goal:** Mark attendance → auto-generate certificate. Full student lifecycle complete.

### What to Build

#### 5.1 Attendance Service (`attendance-service/`)

**Entity: `Attendance`**
```
id, registrationId (unique), studentId, studentName, studentEmail,
eventId, eventTitle, markedAt, status (PRESENT/ABSENT)
```

**Sync call (Feign):**
- `RegistrationClient` → GET `/api/registrations/{id}/exists` — validate registration exists
- Fallback: throws `RegistrationServiceUnavailableException` (503)

**RabbitMQ Publisher:**
- Exchange: `campus.events`
- Routing key: `attendance.completed`
- Payload: `{ attendanceId, registrationId, studentId, studentName, studentEmail, eventId, eventTitle, markedAt }`

**Endpoints:**
```
POST  /api/attendance                         → Mark attendance (validates registration)
GET   /api/attendance/{id}                    → Get attendance record by ID
GET   /api/attendance/{registrationId}/status → Check if student attended (returns present bool + timestamp)
GET   /api/attendance/event/{eventId}         → List attendees for event
GET   /api/attendance/student/{studentId}     → Student's attendance history
```

**Database:** `attendance_db`

#### 5.2 Certificate Service (`certificate-service/`)

**Entity: `Certificate`**
```
id, studentId, studentName, eventId, eventTitle,
registrationId (unique), certificateNumber (unique UUID), issuedAt, pdfData (byte[])
```

**RabbitMQ Consumer:**
- Queue: `campus.certificate.queue`
- Binding: `attendance.completed` routing key
- On receive: idempotently generate PDF certificate using PDFBox
- Certificate includes: student name, event title, certificate number, date

**Endpoints:**
```
GET  /api/certificates/{id}                       → Get certificate metadata
GET  /api/certificates/number/{certNumber}        → Get by certificate number (public verification)
GET  /api/certificates/registration/{regId}       → Get by registration ID
GET  /api/certificates/student/{studentId}        → List student's certificates
GET  /api/certificates/{id}/pdf                   → Download PDF as attachment
```

**Database:** `certificate_db`

### Test Criteria
- [x] attendance-service: 12/12 automated tests pass (mark, duplicate 409, not-found 404, validation 400, get by id, status check, list by event/student)
- [x] certificate-service: 10/10 automated tests pass (generate, idempotency, get by id/number/registration/student, PDF download, 404 handling)
- [x] Attendance generation is idempotent — duplicate `registrationId` returns 409 (automated)
- [x] Certificate generation is idempotent — re-delivering the same message creates no duplicate (automated)
- [x] PDFBox generates a valid non-empty PDF containing student name and event title (automated)
- [x] `GET /api/certificates/{id}/pdf` returns `Content-Type: application/pdf` (automated)
- [ ] Mark attendance → `attendance.completed` message visible in RabbitMQ UI (requires running stack)
- [ ] Certificate row created in `certificate_db` with non-null `pdfData` (requires running stack)
- [ ] Download PDF and verify it renders correctly (requires running stack)

---

## Phase 6 — Engagement Layer ✅

**Goal:** Feedback, Leaderboard, and Announcements complete the event engagement features.

### What to Build

#### 6.1 Feedback Service (`feedback-service/`, port 4078)

**Entity: `Feedback`**
```
id, studentId, studentName, eventId, rating (1-5), comment, submittedAt
unique constraint: (student_id, event_id)
```

**Endpoints:**
```
POST  /api/feedback                    → Submit feedback (one per student per event)
GET   /api/feedback/{id}               → Get feedback by ID
GET   /api/feedback/event/{id}         → All feedback for event
GET   /api/feedback/event/{id}/summary → Aggregated summary (avg rating, distribution)
GET   /api/feedback/student/{id}       → Student's feedback history
```

**Database:** `feedback_db`

#### 6.2 Leaderboard Service (`leaderboard-service/`, port 4079)

**Entity: `Result`**
```
id, eventId, eventTitle, studentId, studentName,
position (FIRST/SECOND/THIRD/PARTICIPANT), category, points, publishedAt
```

**Auto-points:** FIRST=100, SECOND=75, THIRD=50, PARTICIPANT=10 (overridable)

**RabbitMQ Publisher:**
- Exchange: `campus.events`, Routing key: `results.published`

**Endpoints:**
```
POST  /api/leaderboard/results         → Publish result for student (triggers notification)
GET   /api/leaderboard/results/{id}    → Get result by ID
GET   /api/leaderboard/event/{eventId} → Rankings for event (sorted by points)
GET   /api/leaderboard/student/{studentId} → Student's achievements
GET   /api/leaderboard/top?limit=10    → Overall top performers
```

**Database:** `leaderboard_db`

#### 6.3 Announcement Service (`announcement-service/`, port 4080)

**Entity: `Announcement`**
```
id, title, content, eventId (nullable), type (GENERAL/EVENT_UPDATE/VENUE_CHANGE/EMERGENCY),
publishedBy, publishedAt
```

**RabbitMQ Publisher:**
- Exchange: `campus.events`, Routing key: `announcement.created`

**Endpoints:**
```
POST  /api/announcements               → Create announcement (triggers notification broadcast)
GET   /api/announcements/{id}          → Get announcement by ID
GET   /api/announcements               → List all (newest first)
GET   /api/announcements/event/{id}    → Announcements for specific event
GET   /api/announcements/type/{type}   → Filter by type
```

**Database:** `announcement_db`

### Test Criteria
- [x] feedback-service: 12/12 automated tests pass (submit, duplicate 409, validation 400, get by id/event/student, summary avg/distribution)
- [x] leaderboard-service: 11/11 automated tests pass (publish, auto-points, get by id/event/student, top performers, 404, 400)
- [x] announcement-service: 10/10 automated tests pass (create, validation, get by id/all/event/type, 404, default type)
- [x] Feedback prevents duplicate submissions per student per event (automated)
- [x] Leaderboard auto-assigns points based on position (automated)
- [x] Summary endpoint returns correct average and distribution across multiple responses (automated)
- [ ] Publish leaderboard results → `results.published` message visible in RabbitMQ UI (requires running stack)
- [ ] Notification Service receives and logs results notification (requires running stack)
- [ ] Create announcement → `announcement.created` in RabbitMQ → notification created (requires running stack)

---

## Phase 7 — Utility Services ✅

**Goal:** File uploads and sponsor management round out the platform.

### What to Build

#### 7.1 Resource Upload Service (`resource-service/`, port 4081)

**Entity: `Resource`**
```
id, eventId, uploadedBy, fileName, fileType, fileSize,
storageKey (UUID-prefixed, unique), description, uploadedAt
```

**Storage:** Local filesystem under configurable `resource.upload-dir` (default `uploads/`). Swappable for S3 in production via `StorageService` abstraction.

**Endpoints:**
```
POST   /api/resources/upload           → Upload file (multipart/form-data, max 10MB)
GET    /api/resources/{id}             → Get resource metadata
GET    /api/resources/event/{eventId}  → List resources for event
GET    /api/resources/{id}/download    → Download file (Content-Disposition attachment)
DELETE /api/resources/{id}             → Delete resource (removes file + DB row)
```

**Database:** `resource_db`

#### 7.2 Sponsor Service (`sponsor-service/`, port 4082)

**Entity: `Sponsor`**
```
id, name, logoUrl, website, tier (PLATINUM/GOLD/SILVER/BRONZE),
contactPerson, contactEmail, description
```

**Entity: `EventSponsor`**
```
id, eventId, sponsorId (FK), contribution (BigDecimal), notes, linkedAt
unique constraint: (event_id, sponsor_id)
```

**Endpoints:**
```
POST  /api/sponsors                        → Create sponsor
GET   /api/sponsors                        → List all sponsors
GET   /api/sponsors/{id}                   → Get sponsor detail
PUT   /api/sponsors/{id}                   → Update sponsor
POST  /api/sponsors/{id}/events/{eventId}  → Link sponsor to event
GET   /api/sponsors/event/{eventId}        → Sponsors for an event (with contribution details)
```

**Database:** `sponsor_db`

### Test Criteria
- [x] resource-service: 11/11 automated tests pass (upload, get by id/event, download, delete, size limit 413, 404 after delete)
- [x] sponsor-service: 12/12 automated tests pass (create, default tier, validation 400, get/update/list, link, duplicate link 409, get by event, 404)
- [x] File size limit enforced — files > 10MB return 413 (automated)
- [x] Duplicate sponsor-event link returns 409 (automated)
- [x] Download returns correct `Content-Disposition` and `Content-Type` headers (automated)
- [ ] Upload a file → stored on disk, row in `resource_db` (requires running stack)
- [ ] Download file by ID returns correct bytes (requires running stack)

---

## Phase 8 — Containerization

**Goal:** `docker-compose up` starts the entire system with one command.

### What to Build

#### 8.1 Dockerfile (per service)
Each service gets a multi-stage Dockerfile:
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 8.2 Docker Compose (`docker-compose.yml`)
Services:
- `rabbitmq` — `rabbitmq:3.12-management`
- `postgres-event`, `postgres-registration`, ..., `postgres-sponsor` (12 PostgreSQL instances)
- `eureka-server`
- `api-gateway` (depends on eureka)
- All 12 business services (depend on their DB + eureka)

Networks: `campus-network` (bridge)
Volumes: One named volume per PostgreSQL instance

#### 8.3 Environment configuration
Each service's `application.yml` uses environment variables:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/event_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://localhost:4070/eureka}
```

### Test Criteria
- [ ] `docker-compose up --build` starts all containers without error
- [ ] All 14 services show `UP` in Eureka at `http://localhost:4070`
- [ ] RabbitMQ UI accessible at `http://localhost:15672`
- [ ] Full demo flow works end-to-end via `http://localhost:4069`
- [ ] Stopping `event-service` container → registration returns circuit breaker response
- [ ] Restarting `event-service` container → it re-registers in Eureka, system recovers

---

## Phase 9 — Kubernetes Deployment

**Goal:** Full system deployable to Kubernetes cluster.

### What to Build

#### 9.1 Namespace + ConfigMap
```
k8s/
├── namespace.yaml
├── configmap.yaml          ← shared env vars (RabbitMQ host, Eureka URL)
├── eureka/
│   ├── deployment.yaml
│   └── service.yaml
├── gateway/
│   ├── deployment.yaml
│   └── service.yaml (LoadBalancer)
└── services/
    ├── event-service/
    │   ├── deployment.yaml
    │   ├── service.yaml
    │   └── postgres-deployment.yaml
    └── ... (same pattern for each service)
```

#### 9.2 Per-service Kubernetes resources
Each service:
- `Deployment` with 1 replica, liveness/readiness probes
- `Service` (ClusterIP)
- `Deployment` + `Service` for its PostgreSQL pod

#### 9.3 Gateway exposed via LoadBalancer / NodePort

### Failure Demo
```bash
# Kill a pod — Kubernetes restarts it
kubectl delete pod <event-service-pod> -n campus-eventhub

# Watch it come back
kubectl get pods -n campus-eventhub -w
```

### Test Criteria
- [ ] `kubectl apply -f k8s/` deploys all resources
- [ ] All pods reach `Running` state
- [ ] `kubectl get svc api-gateway` shows external IP / NodePort
- [ ] Demo flow works through Kubernetes gateway
- [ ] Kill a pod → pod restarts automatically (verify with `kubectl get pods -w`)
- [ ] HPA demo (optional): scale registration-service under load

---

## Phase 10 — Frontend Dashboard (Optional)

**Goal:** Simple UI for demo purposes showing the full flow visually.

### Options

**Option A: Thymeleaf (simpler)**
- Add Thymeleaf templates to API Gateway or a dedicated frontend service
- Server-side rendered pages, no separate build step

**Option B: React (richer)**
- Separate `frontend/` directory
- Vite + React + Axios
- Pages: Events list, Registration form, My Tickets, Attendance, My Certificates

### Key Pages
- `/events` — Browse events, view details, register
- `/my-registrations` — Student's registrations + QR codes
- `/attendance/{eventId}` — Organizer marks attendance
- `/certificates` — Download certificates
- `/feedback/{eventId}` — Submit feedback
- `/leaderboard` — Competition results
- `/admin` — Event management dashboard

### Test Criteria
- [ ] Full demo flow completable through browser without API client
- [ ] QR code image renders on ticket page
- [ ] Certificate download works from browser

---

## Development Rules

1. **Test before moving on** — Every phase has explicit test criteria. Don't advance until all pass.
2. **One DB per service** — Never share a database between services. No cross-schema queries.
3. **Feign + Resilience4j always** — All synchronous inter-service calls use Feign with a circuit breaker.
4. **Environment variables for config** — No hardcoded hosts/ports. Use `${VAR:default}` pattern.
5. **Consistent port assignment** — Follow the port map in README. No conflicts.
6. **Idempotent consumers** — RabbitMQ consumers must handle duplicate messages safely.
7. **Health endpoints** — Every service exposes `/actuator/health` for container probes.
