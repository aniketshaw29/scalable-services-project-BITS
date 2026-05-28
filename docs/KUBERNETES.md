# Kubernetes Deployment Guide

This guide covers deploying Campus EventHub to a local Kubernetes cluster using either **minikube** or **Docker Desktop**.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Architecture in Kubernetes](#architecture-in-kubernetes)
- [Step-by-Step Deployment](#step-by-step-deployment)
- [Verifying the Deployment](#verifying-the-deployment)
- [Accessing Services](#accessing-services)
- [Demo Flow](#demo-flow)
- [Troubleshooting](#troubleshooting)
- [Teardown](#teardown)
- [Manifest Reference](#manifest-reference)

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Docker | 20+ | [docs.docker.com](https://docs.docker.com/get-docker/) |
| kubectl | 1.27+ | [kubernetes.io/docs](https://kubernetes.io/docs/tasks/tools/) |
| minikube (option A) | 1.30+ | `brew install minikube` |
| Docker Desktop k8s (option B) | 4.20+ | Enable in Docker Desktop → Settings → Kubernetes |
| Java 17 | 17+ | See [INSTALL.md](INSTALL.md) |
| Maven | 3.8+ | See [INSTALL.md](INSTALL.md) |

Verify kubectl is connected to your cluster:

```bash
kubectl cluster-info
```

---

## Quick Start

```bash
# Option A — minikube
minikube start --memory=8192 --cpus=4

# Option B — Docker Desktop: enable Kubernetes in settings, then:
kubectl config use-context docker-desktop

# Deploy everything
./deploy.sh

# Watch pods come up
kubectl get pods -n campus-eventhub --watch
```

Once all pods are `Running` and `1/1 Ready`:

```bash
# Access the API Gateway
curl http://localhost:30069/actuator/health
```

---

## Architecture in Kubernetes

```
                        ┌─────────────────────────────────┐
                        │      campus-eventhub namespace   │
                        │                                  │
  external ──NodePort───►  api-gateway (30069)             │
                        │       │                          │
                        │  eureka-server (4070)            │
                        │       │                          │
                        │  rabbitmq (5672/15672)           │
                        │                                  │
                        │  ┌──────────────────────────┐   │
                        │  │  12 business services     │   │
                        │  │  each with own postgres   │   │
                        │  └──────────────────────────┘   │
                        └─────────────────────────────────┘
```

All inter-service communication is via ClusterIP. Only the API Gateway is exposed externally via NodePort `30069`.

---

## Step-by-Step Deployment

### 1. Start the cluster

**minikube:**
```bash
minikube start --memory=8192 --cpus=4

# Point local Docker to minikube's Docker daemon (required for local images)
eval $(minikube docker-env)
```

**Docker Desktop:** Enable Kubernetes in Settings → Kubernetes → Enable Kubernetes, then:
```bash
kubectl config use-context docker-desktop
```

### 2. Build Docker images

`deploy.sh` handles this automatically, but you can also build manually:

```bash
# Build all images (run from project root)
for svc in eureka-server api-gateway event-service registration-service venue-service \
           attendance-service ticket-service notification-service certificate-service \
           feedback-service leaderboard-service announcement-service resource-service \
           sponsor-service; do
  docker build -t campus-eventhub/${svc}:latest ${svc}/
done
```

> **minikube note:** Always build images *after* running `eval $(minikube docker-env)` so they land in minikube's registry, not your host's Docker daemon.

### 3. Deploy

```bash
./deploy.sh
```

The script:
1. Detects minikube and switches Docker env automatically
2. Builds all 14 Docker images
3. Creates the `campus-eventhub` namespace
4. Applies the ConfigMap
5. Deploys RabbitMQ and waits for it to be ready
6. Deploys Eureka and waits for it to be ready
7. Deploys the API Gateway
8. Deploys all 12 business services (with their PostgreSQL instances)

To skip rebuilding images (e.g., after code hasn't changed):
```bash
./deploy.sh --skip-build
```

---

## Verifying the Deployment

### Check all pods

```bash
kubectl get pods -n campus-eventhub
```

Expected output (all `1/1 Running`):

```
NAME                                    READY   STATUS    RESTARTS   AGE
announcement-db-xxxx                    1/1     Running   0          3m
announcement-service-xxxx               1/1     Running   0          3m
api-gateway-xxxx                        1/1     Running   0          2m
attendance-db-xxxx                      1/1     Running   0          3m
attendance-service-xxxx                 1/1     Running   0          3m
certificate-db-xxxx                     1/1     Running   0          3m
certificate-service-xxxx                1/1     Running   0          3m
eureka-server-xxxx                      1/1     Running   0          4m
event-db-xxxx                           1/1     Running   0          3m
event-service-xxxx                      1/1     Running   0          3m
feedback-db-xxxx                        1/1     Running   0          3m
feedback-service-xxxx                   1/1     Running   0          3m
leaderboard-db-xxxx                     1/1     Running   0          3m
leaderboard-service-xxxx                1/1     Running   0          3m
notification-db-xxxx                    1/1     Running   0          3m
notification-service-xxxx               1/1     Running   0          3m
rabbitmq-xxxx                           1/1     Running   0          5m
registration-db-xxxx                    1/1     Running   0          3m
registration-service-xxxx               1/1     Running   0          3m
resource-db-xxxx                        1/1     Running   0          3m
resource-service-xxxx                   1/1     Running   0          3m
sponsor-db-xxxx                         1/1     Running   0          3m
sponsor-service-xxxx                    1/1     Running   0          3m
ticket-db-xxxx                          1/1     Running   0          3m
ticket-service-xxxx                     1/1     Running   0          3m
venue-db-xxxx                           1/1     Running   0          3m
venue-service-xxxx                      1/1     Running   0          3m
```

### Check services

```bash
kubectl get services -n campus-eventhub
```

### Health check via gateway

```bash
curl http://localhost:30069/actuator/health
```

### Check Eureka registrations

```bash
kubectl port-forward -n campus-eventhub svc/eureka-server 4070:4070 &
open http://localhost:4070   # or: curl http://localhost:4070
```

All 13 services (excluding Eureka itself) should appear as `UP`.

---

## Accessing Services

### API Gateway (primary access point)

The API Gateway is exposed on NodePort `30069`:

```
http://localhost:30069
```

All API calls go through the gateway:

```bash
# Example: list events
curl http://localhost:30069/api/events

# Example: create an event
curl -X POST http://localhost:30069/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Tech Summit","description":"Annual tech event","date":"2026-06-15T10:00:00","capacity":200,"venue":{"id":1}}'
```

### Eureka Dashboard

```bash
kubectl port-forward -n campus-eventhub svc/eureka-server 4070:4070
```
Visit [http://localhost:4070](http://localhost:4070)

### RabbitMQ Management UI

```bash
kubectl port-forward -n campus-eventhub svc/rabbitmq 15672:15672
```
Visit [http://localhost:15672](http://localhost:15672) — username/password: `guest`/`guest`

### Access any individual service directly

```bash
# Port-forward to any service
kubectl port-forward -n campus-eventhub svc/event-service 4071:4071
curl http://localhost:4071/actuator/health
```

### minikube: get NodePort URL

If using minikube, `localhost` may not resolve to the node IP. Use:
```bash
minikube service api-gateway -n campus-eventhub --url
```

---

## Demo Flow

A complete end-to-end demo to verify all services are working:

```bash
GW="http://localhost:30069"

# 1. Create a venue
VENUE=$(curl -s -X POST $GW/api/venues \
  -H "Content-Type: application/json" \
  -d '{"name":"Main Hall","location":"Building A","capacity":300}')
VENUE_ID=$(echo $VENUE | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Venue created: $VENUE_ID"

# 2. Create an event
EVENT=$(curl -s -X POST $GW/api/events \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Demo Day\",\"description\":\"Live demo\",\"date\":\"2026-07-01T14:00:00\",\"capacity\":100,\"venueId\":$VENUE_ID}")
EVENT_ID=$(echo $EVENT | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Event created: $EVENT_ID"

# 3. Register a participant
REG=$(curl -s -X POST $GW/api/registrations \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":$EVENT_ID,\"participantName\":\"Alice\",\"participantEmail\":\"alice@example.com\"}")
REG_ID=$(echo $REG | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Registration created: $REG_ID"

# 4. Mark attendance
curl -s -X POST $GW/api/attendance \
  -H "Content-Type: application/json" \
  -d "{\"registrationId\":$REG_ID,\"eventId\":$EVENT_ID}"

# 5. Check leaderboard
curl -s $GW/api/leaderboard/top | python3 -m json.tool

# 6. Check notifications were sent
curl -s $GW/api/notifications | python3 -m json.tool

# 7. Check certificate was issued
curl -s $GW/api/certificates/registration/$REG_ID | python3 -m json.tool
```

---

## Troubleshooting

### Pod stuck in `Pending`

```bash
kubectl describe pod -n campus-eventhub <pod-name>
```

Common causes:
- Insufficient memory: increase minikube memory with `minikube start --memory=10240`
- PVC cannot be bound: check `kubectl get pvc -n campus-eventhub`

### Pod in `CrashLoopBackOff`

```bash
kubectl logs -n campus-eventhub deployment/<service-name> --previous
```

Common causes:
- Database not ready yet: pods will restart and eventually succeed once the DB readiness probe passes
- Missing ConfigMap key: check `kubectl describe configmap campus-config -n campus-eventhub`

### Service not appearing in Eureka

- The service's readiness probe may still be failing: `kubectl get pods -n campus-eventhub`
- Spring Boot startup can take 60–90s after the container starts: wait and refresh

### Cannot reach `localhost:30069`

**minikube:** NodePort on `localhost` doesn't work on all OS. Use:
```bash
minikube service api-gateway -n campus-eventhub --url
```

**Docker Desktop:** Ensure Kubernetes is enabled and you're on the `docker-desktop` context:
```bash
kubectl config current-context
```

### Image pull errors (`ErrImagePull` / `ImagePullBackOff`)

```bash
kubectl describe pod -n campus-eventhub <pod-name> | grep -A5 Events
```

- All images use `imagePullPolicy: IfNotPresent`, so they must be built locally first
- For minikube: make sure you ran `eval $(minikube docker-env)` before building

### Reset and redeploy a single service

```bash
kubectl rollout restart deployment/<service-name> -n campus-eventhub
kubectl rollout status deployment/<service-name> -n campus-eventhub
```

### Force delete a stuck pod

```bash
kubectl delete pod -n campus-eventhub <pod-name> --force --grace-period=0
```

---

## Teardown

Remove all deployments and services (keeps PVCs and data):
```bash
./teardown.sh
```

Remove everything including data (destructive):
```bash
./teardown.sh --delete-namespace
```

Or manually:
```bash
# Delete all resources in the namespace
kubectl delete all --all -n campus-eventhub

# Delete PVCs (data loss)
kubectl delete pvc --all -n campus-eventhub

# Delete the namespace
kubectl delete namespace campus-eventhub
```

Stop minikube:
```bash
minikube stop
# or delete entirely:
minikube delete
```

---

## Manifest Reference

| File | Contents |
|------|----------|
| [k8s/namespace.yaml](../k8s/namespace.yaml) | Namespace `campus-eventhub` |
| [k8s/configmap.yaml](../k8s/configmap.yaml) | Shared config (DB creds, RabbitMQ, Eureka URL) |
| [k8s/rabbitmq/rabbitmq.yaml](../k8s/rabbitmq/rabbitmq.yaml) | RabbitMQ Deployment + PVC + Service |
| [k8s/eureka/eureka.yaml](../k8s/eureka/eureka.yaml) | Eureka Server Deployment + Service |
| [k8s/gateway/gateway.yaml](../k8s/gateway/gateway.yaml) | API Gateway Deployment + NodePort Service |
| [k8s/services/event-service/](../k8s/services/event-service/) | Event Service + Postgres |
| [k8s/services/registration-service/](../k8s/services/registration-service/) | Registration Service + Postgres |
| [k8s/services/venue-service/](../k8s/services/venue-service/) | Venue Service + Postgres |
| [k8s/services/attendance-service/](../k8s/services/attendance-service/) | Attendance Service + Postgres |
| [k8s/services/ticket-service/](../k8s/services/ticket-service/) | Ticket Service + Postgres |
| [k8s/services/notification-service/](../k8s/services/notification-service/) | Notification Service + Postgres |
| [k8s/services/certificate-service/](../k8s/services/certificate-service/) | Certificate Service + Postgres |
| [k8s/services/feedback-service/](../k8s/services/feedback-service/) | Feedback Service + Postgres |
| [k8s/services/leaderboard-service/](../k8s/services/leaderboard-service/) | Leaderboard Service + Postgres |
| [k8s/services/announcement-service/](../k8s/services/announcement-service/) | Announcement Service + Postgres |
| [k8s/services/resource-service/](../k8s/services/resource-service/) | Resource Service + Postgres + uploads PVC |
| [k8s/services/sponsor-service/](../k8s/services/sponsor-service/) | Sponsor Service + Postgres |

### Port reference

| Service | Container Port | NodePort |
|---------|---------------|----------|
| api-gateway | 4069 | **30069** |
| eureka-server | 4070 | — (ClusterIP) |
| event-service | 4071 | — |
| registration-service | 4072 | — |
| venue-service | 4073 | — |
| attendance-service | 4074 | — |
| ticket-service | 4075 | — |
| notification-service | 4076 | — |
| certificate-service | 4077 | — |
| feedback-service | 4078 | — |
| leaderboard-service | 4079 | — |
| announcement-service | 4080 | — |
| resource-service | 4081 | — |
| sponsor-service | 4082 | — |
| rabbitmq (amqp) | 5672 | — |
| rabbitmq (management) | 15672 | — |
