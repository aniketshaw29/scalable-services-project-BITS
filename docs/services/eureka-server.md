# Eureka Server

**Port:** 4070
**Role:** Service registry — all other services register here and discover each other by name.

---

## Overview

The Eureka Server is the central nervous system of the Campus EventHub platform. Every microservice registers itself with Eureka on startup and periodically sends heartbeats to stay registered. Other services (and the API Gateway) look up service instances by name instead of hardcoded URLs, enabling load balancing and zero-downtime restarts.

This service has **no business logic** and **no database**. It is purely infrastructure.

---

## Configuration

```yaml
server:
  port: ${SERVER_PORT:4070}

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: ${EUREKA_HOSTNAME:localhost}
  client:
    register-with-eureka: false   # does not register itself
    fetch-registry: false         # does not fetch the registry
  server:
    wait-time-in-ms-when-sync-empty: 0
    enable-self-preservation: false
```

`enable-self-preservation: false` is safe for development — in production you would leave this on to prevent mass evictions during network partitions.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `4070` | HTTP port |
| `EUREKA_HOSTNAME` | `localhost` | Hostname used in self-registration URL |

In Docker Compose, `EUREKA_HOSTNAME` is set to `eureka-server` (the container name) so other containers can resolve it.

---

## Dashboard

Open `http://localhost:4070` in a browser. You will see all registered service instances, their status, and last heartbeat time.

A healthy deployment shows all 12 business services + `API-GATEWAY` as `UP`.

---

## Key Dependency Direction

```
All services → register with → Eureka Server
API Gateway  → discovers routes via → Eureka Server
```

Eureka must be the **first service started**. All others depend on it.

---

## Health Check

```
GET http://localhost:4070/actuator/health
```

Returns `{"status":"UP"}` when ready.

---

## Technology

- Spring Boot 3.2.5
- Spring Cloud Netflix Eureka Server
- No database, no RabbitMQ, no Feign clients
