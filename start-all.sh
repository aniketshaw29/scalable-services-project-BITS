#!/usr/bin/env bash
# start-all.sh — start every Campus EventHub service locally
# Run from the project root: ./start-all.sh
#
# Options:
#   --skip-build   skip mvn package (use existing JARs)
#   --stop         kill all running services and exit
#   --logs         tail logs after starting (Ctrl+C to stop tailing, services keep running)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"
PID_FILE="$ROOT/.service-pids"

# ── colour helpers ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }

# ── service list (start order matters) ──────────────────────────────────────
SERVICES=(
  "eureka-server"
  "api-gateway"
  "event-service"
  "venue-service"
  "registration-service"
  "attendance-service"
  "ticket-service"
  "notification-service"
  "certificate-service"
  "feedback-service"
  "leaderboard-service"
  "announcement-service"
  "resource-service"
  "sponsor-service"
)

# seconds to wait between each service start to allow Eureka registration
STARTUP_DELAY=4

# ── --stop flag ──────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--stop" ]]; then
  if [[ ! -f "$PID_FILE" ]]; then
    warn "No PID file found ($PID_FILE). Nothing to stop."
    exit 0
  fi
  info "Stopping all services..."
  while IFS= read -r line; do
    name="${line%%:*}"
    pid="${line##*:}"
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" && info "Stopped $name (PID $pid)"
    else
      warn "$name (PID $pid) was not running"
    fi
  done < "$PID_FILE"
  rm -f "$PID_FILE"
  info "All services stopped."
  exit 0
fi

# ── parse flags ──────────────────────────────────────────────────────────────
SKIP_BUILD=false
TAIL_LOGS=false
for arg in "$@"; do
  case $arg in
    --skip-build) SKIP_BUILD=true ;;
    --logs)       TAIL_LOGS=true ;;
  esac
done

# ── pre-flight checks ────────────────────────────────────────────────────────
command -v java  >/dev/null 2>&1 || { error "java not found. Install Java 17+."; exit 1; }
command -v mvn   >/dev/null 2>&1 || { error "mvn not found. Install Maven 3.8+."; exit 1; }

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [[ "$JAVA_VER" -lt 17 ]]; then
  error "Java 17+ required. Found Java $JAVA_VER."
  exit 1
fi

# ── build ────────────────────────────────────────────────────────────────────
if [[ "$SKIP_BUILD" == false ]]; then
  info "Building all services (mvn clean package -DskipTests)..."
  cd "$ROOT"
  mvn clean package -DskipTests -q
  info "Build complete."
else
  warn "--skip-build: using existing JARs."
fi

# ── create log dir and clear old PIDs ────────────────────────────────────────
mkdir -p "$LOG_DIR"
rm -f "$PID_FILE"

# ── start services ───────────────────────────────────────────────────────────
info "Starting services (delay: ${STARTUP_DELAY}s between each)..."
echo ""

for svc in "${SERVICES[@]}"; do
  JAR=$(find "$ROOT/$svc/target" -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)
  if [[ -z "$JAR" ]]; then
    error "No JAR found for $svc in $ROOT/$svc/target/. Run without --skip-build."
    exit 1
  fi

  LOG_FILE="$LOG_DIR/$svc.log"
  java -jar "$JAR" > "$LOG_FILE" 2>&1 &
  PID=$!
  echo "${svc}:${PID}" >> "$PID_FILE"
  info "Started $svc  (PID $PID) → logs/$svc.log"

  sleep "$STARTUP_DELAY"
done

echo ""
info "All ${#SERVICES[@]} services started."
echo ""
echo "  Access points:"
echo "    API Gateway   →  http://localhost:4069"
echo "    Eureka UI     →  http://localhost:4070"
echo "    RabbitMQ UI   →  http://localhost:15672  (guest / guest)"
echo ""
echo "  Commands:"
echo "    ./start-all.sh --stop       stop all services"
echo "    ./start-all.sh --skip-build restart without rebuilding"
echo "    tail -f logs/<service>.log  watch a service log"
echo ""

# ── optional log tailing ─────────────────────────────────────────────────────
if [[ "$TAIL_LOGS" == true ]]; then
  info "Tailing all logs (Ctrl+C to stop tailing — services keep running)..."
  tail -f "$LOG_DIR"/*.log
fi
