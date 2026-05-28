# Campus EventHub — Documentation Index

Complete documentation for the Campus EventHub microservices platform.

---

## Getting Started

| Guide | Description |
|-------|-------------|
| [Install Prerequisites](INSTALL.md) | Install Java, Maven, PostgreSQL, RabbitMQ on macOS/Linux/Windows |
| [Running Locally](RUNNING_LOCALLY.md) | Run all services on your machine without Docker |
| [Docker Compose](DOCKER.md) | Spin up the entire stack with one command |
| [Kubernetes Deployment](KUBERNETES.md) | Deploy to a local Kubernetes cluster (minikube / Docker Desktop) |

---

## Architecture & Design

| Document | Description |
|----------|-------------|
| [Architecture Reference](ARCHITECTURE.md) | Principles, data models, Feign patterns, RabbitMQ topology |
| [Message Contracts](MESSAGE_CONTRACTS.md) | RabbitMQ exchange, queue bindings, message payloads |
| [API Contracts](API_CONTRACTS.md) | All HTTP endpoints, request/response shapes, status codes |
| [Development Plan](DEVELOPMENT_PLAN.md) | Phased build plan, test criteria, progress status |

---

## Services

### Infrastructure

| Service | Port | Document |
|---------|------|----------|
| Eureka Server | 4070 | [eureka-server.md](services/eureka-server.md) |
| API Gateway | 4069 | [api-gateway.md](services/api-gateway.md) |

### Business Services

| Service | Port | Responsibility | Document |
|---------|------|----------------|----------|
| Event Service | 4071 | Create and manage events | [event-service.md](services/event-service.md) |
| Venue Service | 4073 | Manage venues and bookings | [venue-service.md](services/venue-service.md) |
| Registration Service | 4072 | Student event registration | [registration-service.md](services/registration-service.md) |
| Attendance Service | 4074 | Mark and track attendance | [attendance-service.md](services/attendance-service.md) |
| Ticket Service | 4075 | Generate QR pass tickets | [ticket-service.md](services/ticket-service.md) |
| Notification Service | 4076 | Confirmation and alert logs | [notification-service.md](services/notification-service.md) |
| Certificate Service | 4077 | PDF certificate generation | [certificate-service.md](services/certificate-service.md) |
| Feedback Service | 4078 | Ratings, comments, summaries | [feedback-service.md](services/feedback-service.md) |
| Leaderboard Service | 4079 | Competition results, rankings | [leaderboard-service.md](services/leaderboard-service.md) |
| Announcement Service | 4080 | Event announcements | [announcement-service.md](services/announcement-service.md) |
| Resource Service | 4081 | File uploads and downloads | [resource-service.md](services/resource-service.md) |
| Sponsor Service | 4082 | Sponsor management | [sponsor-service.md](services/sponsor-service.md) |

---

## Quick Links

- **API entry point:** `http://localhost:4069`
- **Eureka dashboard:** `http://localhost:4070`
- **RabbitMQ management UI:** `http://localhost:15672` (guest / guest)
- **Source:** [GitHub](https://github.com)
