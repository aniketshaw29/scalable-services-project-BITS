# API Gateway

**Port:** 4069
**Role:** Single entry point for all client requests — routes to the correct service by URL path.

---

## Overview

The API Gateway is the only service exposed to the outside world. Clients (frontend, Postman, curl) send all requests to `http://localhost:4069`, and the gateway forwards them to the correct downstream service using Spring Cloud Gateway's path-based routing combined with Eureka-based load balancing (`lb://`).

The gateway also handles global CORS, so all services benefit without configuring CORS individually.

This service has **no business logic** and **no database**.

---

## Route Table

| Path Prefix | Upstream Service | Port |
|-------------|-----------------|------|
| `/api/events/**` | event-service | 4071 |
| `/api/registrations/**` | registration-service | 4072 |
| `/api/venues/**` | venue-service | 4073 |
| `/api/attendance/**` | attendance-service | 4074 |
| `/api/tickets/**` | ticket-service | 4075 |
| `/api/notifications/**` | notification-service | 4076 |
| `/api/certificates/**` | certificate-service | 4077 |
| `/api/feedback/**` | feedback-service | 4078 |
| `/api/leaderboard/**` | leaderboard-service | 4079 |
| `/api/announcements/**` | announcement-service | 4080 |
| `/api/resources/**` | resource-service | 4081 |
| `/api/sponsors/**` | sponsor-service | 4082 |

Routes use `lb://service-name` URIs — Spring Cloud Gateway resolves these through Eureka and load-balances across instances.

---

## Configuration

```yaml
server:
  port: 4069

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
            allowedHeaders: "*"

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:4070/eureka/}
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `EUREKA_URL` | `http://localhost:4070/eureka/` | Eureka registration endpoint |

---

## Health Check

```
GET http://localhost:4069/actuator/health
```

---

## Technology

- Spring Boot 3.2.5
- Spring Cloud Gateway (reactive, built on Netty — not Tomcat)
- Spring Cloud Netflix Eureka Client
- No database, no RabbitMQ
