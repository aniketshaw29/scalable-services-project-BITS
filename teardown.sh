#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
#  teardown.sh — remove all campus-eventhub resources from Kubernetes
# ─────────────────────────────────────────────

NAMESPACE="campus-eventhub"
DELETE_NAMESPACE="${DELETE_NAMESPACE:-false}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()   { echo -e "${CYAN}[teardown]${NC} $*"; }
ok()    { echo -e "${GREEN}[   ok   ]${NC} $*"; }
warn()  { echo -e "${YELLOW}[  warn  ]${NC} $*"; }
error() { echo -e "${RED}[ error  ]${NC} $*" >&2; }

usage() {
  echo "Usage: $0 [--delete-namespace] [--help]"
  echo ""
  echo "  --delete-namespace   Also delete the '$NAMESPACE' namespace (removes PVCs)"
  echo "  --help               Show this help"
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --delete-namespace) DELETE_NAMESPACE=true ;;
    --help|-h) usage ;;
    *) error "Unknown argument: $1"; usage ;;
  esac
  shift
done

if ! kubectl cluster-info &>/dev/null 2>&1; then
  error "No Kubernetes cluster reachable."
  exit 1
fi

if ! kubectl get namespace "$NAMESPACE" &>/dev/null 2>&1; then
  warn "Namespace '$NAMESPACE' does not exist — nothing to remove."
  exit 0
fi

K8S_BASE="k8s"

delete_if_exists() {
  local path="$1"
  if [[ -f "$path" ]]; then
    kubectl delete -f "$path" --ignore-not-found=true
  fi
}

# ── confirmation prompt ───────────────────────
if [[ "$DELETE_NAMESPACE" == "true" ]]; then
  echo -e "${RED}WARNING: This will delete the '$NAMESPACE' namespace including all PersistentVolumeClaims.${NC}"
  echo -e "${RED}         All database data and uploaded files will be permanently lost.${NC}"
else
  echo -e "${YELLOW}This will remove all deployments and services in '$NAMESPACE' (PVCs kept).${NC}"
fi
echo ""
read -r -p "Are you sure? [y/N] " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
  log "Aborted."
  exit 0
fi

echo ""
log "Starting teardown ..."

if [[ "$DELETE_NAMESPACE" == "true" ]]; then
  log "Deleting namespace '$NAMESPACE' (this removes everything including PVCs) ..."
  kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
  ok "Namespace deleted."
  exit 0
fi

# ── delete in reverse apply order ────────────

ORDERED_SERVICES=(
  announcement-service
  leaderboard-service
  certificate-service
  notification-service
  ticket-service
  attendance-service
  registration-service
  resource-service
  sponsor-service
  feedback-service
  venue-service
  event-service
)

log "Deleting business services ..."
for svc in "${ORDERED_SERVICES[@]}"; do
  manifest="${K8S_BASE}/services/${svc}/${svc}.yaml"
  delete_if_exists "$manifest"
  ok "  Removed $svc"
done

log "Deleting API Gateway ..."
delete_if_exists "${K8S_BASE}/gateway/gateway.yaml"
ok "API Gateway removed"

log "Deleting Eureka ..."
delete_if_exists "${K8S_BASE}/eureka/eureka.yaml"
ok "Eureka removed"

log "Deleting RabbitMQ ..."
delete_if_exists "${K8S_BASE}/rabbitmq/rabbitmq.yaml"
ok "RabbitMQ removed"

log "Deleting ConfigMap ..."
delete_if_exists "${K8S_BASE}/configmap.yaml"
ok "ConfigMap removed"

echo ""
ok "Teardown complete. Namespace '$NAMESPACE' and PVCs were kept."
echo ""
echo "  To also remove the namespace and all PVCs (deletes data):"
echo "    $0 --delete-namespace"
echo ""
echo "  To remove lingering PVCs manually:"
echo "    kubectl get pvc -n $NAMESPACE"
echo "    kubectl delete pvc -n $NAMESPACE --all"
echo ""
