# Testing Guide

Campus EventHub has two layers of automated tests per service. Both layers run in-process using an H2 in-memory database — no running PostgreSQL, RabbitMQ, or Eureka instance is needed.

---

## Test Layers

### Layer 1 — Application tests (existing)

`<service>/<ServiceName>ApplicationTests.java`

Full `@SpringBootTest` + `@AutoConfigureMockMvc` tests that exercise every HTTP endpoint. External dependencies (Feign clients, RabbitMQ publishers) are replaced with `@MockBean` stubs.

| Service | File | Tests |
|---------|------|-------|
| eureka-server | `EurekaServerApplicationTests` | 1 (context load) |
| api-gateway | `ApiGatewayApplicationTests` | 1 (context load) |
| event-service | `EventServiceApplicationTests` | 12 |
| registration-service | `RegistrationServiceApplicationTests` | 14 |
| venue-service | `VenueServiceApplicationTests` | 13 |
| attendance-service | `AttendanceServiceApplicationTests` | 12 |
| ticket-service | `TicketServiceApplicationTests` | 7 |
| notification-service | `NotificationServiceApplicationTests` | 9 |
| certificate-service | `CertificateServiceApplicationTests` | 10 |
| feedback-service | `FeedbackServiceApplicationTests` | 12 |
| leaderboard-service | `LeaderboardServiceApplicationTests` | 11 |
| announcement-service | `AnnouncementServiceApplicationTests` | 10 |
| resource-service | `ResourceServiceApplicationTests` | 11 |
| sponsor-service | `SponsorServiceApplicationTests` | 12 |

### Layer 2 — Integration tests

`<service>/<Name>IntegrationTest.java`

Tests that exercise a specific internal path end-to-end — wiring the service layer, repository, and HTTP API together. For the three RabbitMQ consumer services the tests invoke the consumer directly (bypassing the broker) and assert the resulting DB state and HTTP responses.

| Service | File | Tests | What it covers |
|---------|------|-------|----------------|
| ticket-service | `TicketConsumerIntegrationTest` | 7 | Consumer→DB write, idempotency, HTTP retrieval, mark-used lifecycle |
| notification-service | `NotificationConsumerIntegrationTest` | 7 | All three routing keys, multi-student isolation, unknown event type no-op |
| certificate-service | `CertificateConsumerIntegrationTest` | 7 | PDF generation, idempotency, unique cert numbers, PDF download content-type |
| feedback-service | `FeedbackServiceIntegrationTest` | 10 | Summary avg + distribution, duplicate enforcement, cross-event isolation |
| leaderboard-service | `LeaderboardServiceIntegrationTest` | 11 | Auto-points (all 4 positions), custom points override, ranking sort order, top-performers limit |
| announcement-service | `AnnouncementServiceIntegrationTest` | 10 | Default type fallback, event-scoped + type-based filtering, newest-first ordering |
| attendance-service | `AttendanceServiceIntegrationTest` | 9 | Feign mock + persist + publish in one test, duplicate 409, unknown registration 404, status check |
| sponsor-service | `SponsorServiceIntegrationTest` | 11 | Default tier, update, event linking, duplicate link 409, per-event retrieval |

---

## Running Tests

### All services — one command

```bash
./test-all.sh
```

Runs `mvn test` for all 14 services in order, prints a colour-coded PASS/FAIL summary with elapsed time per service, and exits non-zero if any service fails.

```
── eureka-server
[INFO]  PASS  eureka-server  (18s)

── ticket-service
[INFO]  PASS  ticket-service  (8s)
...
  PASSED (14)
```

### All services — with fail-fast

```bash
./test-all.sh --fail-fast
```

Stops immediately on the first failing service. Useful in CI.

### Single service

```bash
./test-all.sh --service event-service
```

Or directly with Maven:

```bash
mvn -f event-service/pom.xml test
```

### Single test class

```bash
mvn -f ticket-service/pom.xml test \
  -Dtest=TicketConsumerIntegrationTest
```

### Single test method

```bash
mvn -f feedback-service/pom.xml test \
  -Dtest=FeedbackServiceIntegrationTest#summary_multipleRatings_avgCorrectlyComputed
```

### Only integration tests (name pattern)

```bash
mvn -f attendance-service/pom.xml test \
  -Dtest="*IntegrationTest"
```

### Only application tests (exclude integration)

```bash
mvn -f attendance-service/pom.xml test \
  -Dtest="*ApplicationTests"
```

### All services in parallel (faster, uses more CPU)

```bash
mvn test --threads 4
```

---

## Test Configuration

Every service has `src/test/resources/application-test.yml` activated via `@ActiveProfiles("test")`. Key settings:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:<service>_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop        # schema rebuilt on every test run
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

eureka:
  client:
    enabled: false                 # no Eureka server needed
    register-with-eureka: false
    fetch-registry: false

feign:
  circuitbreaker:
    enabled: false                 # circuit breaker disabled in tests
```

RabbitMQ is configured to `localhost:5672` in `application-test.yml` but the `RabbitTemplate` and all publisher beans are replaced with `@MockBean` in every test class — no running RabbitMQ instance is needed.

---

## Test Infrastructure Summary

| Concern | How it's handled |
|---------|-----------------|
| Database | H2 in-memory, `create-drop` per run — no PostgreSQL needed |
| Eureka | Client disabled in test profile |
| Feign clients | `@MockBean` — stubbed with `when(...).thenReturn(...)` |
| RabbitMQ publishers | `@MockBean` — stubbed with `doNothing()` |
| RabbitMQ consumers | Called **directly** in integration tests (no broker needed) |
| RabbitTemplate | `@MockBean` — prevents AMQP connection attempts |
| Context isolation | `@DirtiesContext(classMode = AFTER_CLASS)` per test class |
| Test ordering | `@TestMethodOrder(OrderAnnotation.class)` + `@Order(n)` — stateful tests share one DB within a class |

---

## Viewing Surefire Reports

After `mvn test`, detailed reports are in:

```
<service>/target/surefire-reports/
├── TEST-com.campuseventhub.<service>.*ApplicationTests.xml
├── TEST-com.campuseventhub.<service>.*IntegrationTest.xml
└── com.campuseventhub.<service>.*IntegrationTest.txt   ← human-readable
```

Open a text report:
```bash
cat ticket-service/target/surefire-reports/com.campuseventhub.ticket.TicketConsumerIntegrationTest.txt
```

---

## Total Test Count

| Service | Application tests | Integration tests | Total |
|---------|-------------------|-------------------|-------|
| eureka-server | 1 | — | 1 |
| api-gateway | 1 | — | 1 |
| event-service | 12 | — | 12 |
| registration-service | 14 | — | 14 |
| venue-service | 13 | — | 13 |
| attendance-service | 12 | 9 | **21** |
| ticket-service | 7 | 7 | **14** |
| notification-service | 9 | 7 | **16** |
| certificate-service | 10 | 7 | **17** |
| feedback-service | 12 | 10 | **22** |
| leaderboard-service | 11 | 11 | **22** |
| announcement-service | 10 | 10 | **20** |
| resource-service | 11 | — | 11 |
| sponsor-service | 12 | 11 | **23** |
| **Total** | **135** | **72** | **207** |

<!-- tests reviewed -->
