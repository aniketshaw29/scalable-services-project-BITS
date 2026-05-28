#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
#  deploy.sh — build all Docker images and deploy to local Kubernetes
# ─────────────────────────────────────────────

NAMESPACE="campus-eventhub"
IMAGE_PREFIX="campus-eventhub"
TAG="${TAG:-latest}"
SKIP_BUILD="${SKIP_BUILD:-false}"

SERVICES=(
  eureka-server
  api-gateway
  event-service
  registration-service
  venue-service
  attendance-service
  ticket-service
  notification-service
  certificate-service
  feedback-service
  leaderboard-service
  announcement-service
  resource-service
  sponsor-service
)

K8S_BASE="k8s"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()   { echo -e "${CYAN}[deploy]${NC} $*"; }
ok()    { echo -e "${GREEN}[  ok  ]${NC} $*"; }
warn()  { echo -e "${YELLOW}[ warn ]${NC} $*"; }
error() { echo -e "${RED}[error ]${NC} $*" >&2; }

usage() {
  echo "Usage: $0 [--skip-build] [--tag <tag>] [--help]"
  echo ""
  echo "  --skip-build    Skip Docker image builds (use existing images)"
  echo "  --tag <tag>     Docker image tag to use (default: latest)"
  echo "  --help          Show this help"
  exit 0
}

# ── parse args ───────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=true ;;
    --tag) TAG="$2"; shift ;;
    --help|-h) usage ;;
    *) error "Unknown argument: $1"; usage ;;
  esac
  shift
done

# ── prereqs ──────────────────────────────────
for cmd in kubectl docker mvn; do
  if ! command -v "$cmd" &>/dev/null; then
    error "Required command not found: $cmd"
    exit 1
  fi
done

if ! kubectl cluster-info &>/dev/null 2>&1; then
  error "No Kubernetes cluster reachable. Start minikube or Docker Desktop Kubernetes first."
  exit 1
fi

# ── build images ─────────────────────────────
if [[ "$SKIP_BUILD" == "false" ]]; then
  log "Building Docker images (tag: ${TAG}) ..."

  # If minikube is in use, point Docker to its daemon so images are available
  if command -v minikube &>/dev/null && minikube status &>/dev/null 2>&1; then
    log "Detected minikube — pointing Docker daemon to minikube's Docker"
    eval "$(minikube docker-env)"
  fi

  for svc in "${SERVICES[@]}"; do
    if [[ ! -f "$svc/Dockerfile" ]]; then
      warn "No Dockerfile found for $svc — skipping build"
      continue
    fi
    log "  Building ${IMAGE_PREFIX}/${svc}:${TAG} ..."
    docker build -t "${IMAGE_PREFIX}/${svc}:${TAG}" "$svc"
    ok "  ${IMAGE_PREFIX}/${svc}:${TAG} built"
  done

  ok "All images built."
else
  log "Skipping image builds (--skip-build)"
fi

# ── apply manifests ───────────────────────────
apply() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    warn "Manifest not found: $path — skipping"
    return
  fi
  kubectl apply -f "$path"
}

wait_for_deploy() {
  local name="$1"
  local timeout="${2:-120s}"
  log "  Waiting for deployment/$name (timeout: $timeout) ..."
  if kubectl rollout status deployment/"$name" -n "$NAMESPACE" --timeout="$timeout" &>/dev/null; then
    ok "  $name is ready"
  else
    warn "  $name did not become ready within $timeout — continuing anyway"
  fi
}

log "Applying Kubernetes manifests ..."

# 1. namespace
apply "${K8S_BASE}/namespace.yaml"
ok "Namespace applied"

# 2. configmap
apply "${K8S_BASE}/configmap.yaml"
ok "ConfigMap applied"

# 3. RabbitMQ
apply "${K8S_BASE}/rabbitmq/rabbitmq.yaml"
log "Waiting for RabbitMQ ..."
wait_for_deploy rabbitmq 180s

# 4. Eureka
apply "${K8S_BASE}/eureka/eureka.yaml"
log "Waiting for Eureka ..."
wait_for_deploy eureka-server 180s

# 5. API Gateway
apply "${K8S_BASE}/gateway/gateway.yaml"

# 6. Business services (order: independent first, then dependents)
ORDERED_SERVICES=(
  event-service
  venue-service
  feedback-service
  sponsor-service
  resource-service
  registration-service
  attendance-service
  ticket-service
  notification-service
  certificate-service
  leaderboard-service
  announcement-service
)

for svc in "${ORDERED_SERVICES[@]}"; do
  manifest="${K8S_BASE}/services/${svc}/${svc}.yaml"
  apply "$manifest"
  ok "  Applied $svc"
done

# 7. Wait for gateway (after services so we don't block unnecessarily)
log "Waiting for API Gateway ..."
wait_for_deploy api-gateway 120s

echo ""
ok "Deployment complete!"
echo ""
echo -e "${CYAN}── Access points ────────────────────────────────────────────────${NC}"
echo ""
echo "  API Gateway (NodePort):  http://localhost:30069"
echo "  Eureka Dashboard:        kubectl port-forward -n $NAMESPACE svc/eureka-server 4070:4070"
echo "                           then visit http://localhost:4070"
echo "  RabbitMQ Management UI:  kubectl port-forward -n $NAMESPACE svc/rabbitmq 15672:15672"
echo "                           then visit http://localhost:15672  (guest/guest)"
echo ""
echo -e "${CYAN}── Useful commands ──────────────────────────────────────────────${NC}"
echo ""
echo "  All pods:          kubectl get pods -n $NAMESPACE"
echo "  Watch pods:        kubectl get pods -n $NAMESPACE --watch"
echo "  Service logs:      kubectl logs -n $NAMESPACE deployment/<service-name>"
echo "  Describe pod:      kubectl describe pod -n $NAMESPACE <pod-name>"
echo ""
