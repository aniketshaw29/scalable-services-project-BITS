#!/usr/bin/env bash
# start-all.sh — start every Campus EventHub service locally
# Run from the project root: ./start-all.sh
#
# Options:
#   --skip-build        skip mvn package (use existing JARs)
#   --skip-frontend     skip npm install + frontend dev server
#   --skip-infra-check  skip PostgreSQL / RabbitMQ pre-flight checks
#   --stop              kill all running services and exit
#   --stop <name>       kill one service by name and exit
#   --status            show running/stopped status of all services
#   --restart <name>    stop and restart one service by name
#   --logs              tail logs after starting (Ctrl+C to stop tailing)
#   --logs <name>       tail logs for one specific service

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"
PID_FILE="$ROOT/.service-pids"

# ── colour helpers ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }
step()    { echo -e "${CYAN}[STEP]${NC}  $*"; }
bold()    { echo -e "${BOLD}$*${NC}"; }

# ── Java service list (start order matters) ──────────────────────────────────
JAVA_SERVICES=(
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

ALL_SERVICES=("${JAVA_SERVICES[@]}" "frontend")

# seconds to wait between each Java service start to allow Eureka registration
STARTUP_DELAY=4

# ── helpers ──────────────────────────────────────────────────────────────────

pid_of() {
  local name="$1"
  if [[ ! -f "$PID_FILE" ]]; then echo ""; return; fi
  grep "^${name}:" "$PID_FILE" 2>/dev/null | cut -d: -f2 | head -1
}

is_running() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

stop_one() {
  local name="$1"
  local pid
  pid=$(pid_of "$name")
  if is_running "$pid"; then
    kill "$pid" && info "Stopped $name (PID $pid)"
  else
    warn "$name is not running"
  fi
  # Remove from PID file
  if [[ -f "$PID_FILE" ]]; then
    sed -i.bak "/^${name}:/d" "$PID_FILE" && rm -f "${PID_FILE}.bak"
  fi
}

# ── --stop [name] ────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--stop" ]]; then
  if [[ -n "${2:-}" ]]; then
    # stop one specific service
    stop_one "$2"
  else
    # stop everything
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
  fi
  exit 0
fi

# ── --status ──────────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--status" ]]; then
  bold ""
  bold "  Service Status"
  bold "  ──────────────────────────────────────────"
  printf "  %-30s %s\n" "Service" "Status"
  printf "  %-30s %s\n" "-------" "------"
  for svc in "${ALL_SERVICES[@]}"; do
    pid=$(pid_of "$svc")
    if is_running "$pid"; then
      printf "  %-30s ${GREEN}running${NC}  (PID %s)\n" "$svc" "$pid"
    else
      printf "  %-30s ${RED}stopped${NC}\n" "$svc"
    fi
  done
  echo ""
  exit 0
fi

# ── --restart <name> ──────────────────────────────────────────────────────────
if [[ "${1:-}" == "--restart" ]]; then
  name="${2:-}"
  if [[ -z "$name" ]]; then
    error "--restart requires a service name. Example: ./start-all.sh --restart event-service"
    exit 1
  fi
  info "Restarting $name ..."
  stop_one "$name"
  sleep 1

  mkdir -p "$LOG_DIR"
  LOG_FILE="$LOG_DIR/$name.log"

  if [[ "$name" == "frontend" ]]; then
    FRONTEND_DIR="$ROOT/frontend"
    npm --prefix "$FRONTEND_DIR" run dev > "$LOG_FILE" 2>&1 &
    PID=$!
  else
    JAR=$(find "$ROOT/$name/target" -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)
    if [[ -z "$JAR" ]]; then
      error "No JAR found for $name. Run without --skip-build first."
      exit 1
    fi
    java -jar "$JAR" > "$LOG_FILE" 2>&1 &
    PID=$!
  fi

  echo "${name}:${PID}" >> "$PID_FILE"
  info "Restarted $name (PID $PID) → logs/$name.log"
  exit 0
fi

# ── --logs [name] ────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--logs" ]]; then
  if [[ -n "${2:-}" ]]; then
    LOG_FILE="$LOG_DIR/${2}.log"
    if [[ ! -f "$LOG_FILE" ]]; then
      error "Log file not found: $LOG_FILE"
      exit 1
    fi
    tail -f "$LOG_FILE"
  else
    tail -f "$LOG_DIR"/*.log
  fi
  exit 0
fi

# ── parse remaining flags ─────────────────────────────────────────────────────
SKIP_BUILD=false
SKIP_FRONTEND=false
SKIP_INFRA_CHECK=false
TAIL_LOGS=false
for arg in "$@"; do
  case $arg in
    --skip-build)       SKIP_BUILD=true ;;
    --skip-frontend)    SKIP_FRONTEND=true ;;
    --skip-infra-check) SKIP_INFRA_CHECK=true ;;
    --logs)             TAIL_LOGS=true ;;
  esac
done

# ── pre-flight: Java ──────────────────────────────────────────────────────────
command -v java >/dev/null 2>&1 || { error "java not found. Install Java 21+."; exit 1; }
command -v mvn  >/dev/null 2>&1 || { error "mvn not found. Install Maven 3.8+."; exit 1; }

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [[ "$JAVA_VER" -lt 21 ]]; then
  error "Java 21+ required. Found Java $JAVA_VER."
  exit 1
fi
info "Java $JAVA_VER ✓"

# ── pre-flight: Node ──────────────────────────────────────────────────────────
if [[ "$SKIP_FRONTEND" == false ]]; then
  if ! command -v node >/dev/null 2>&1; then
    warn "node not found — frontend dev server will be skipped. Install Node.js 22+ or pass --skip-frontend."
    SKIP_FRONTEND=true
  elif ! command -v npm >/dev/null 2>&1; then
    warn "npm not found — frontend dev server will be skipped."
    SKIP_FRONTEND=true
  else
    NODE_VER=$(node -e "process.stdout.write(process.version.slice(1).split('.')[0])")
    if [[ "$NODE_VER" -lt 22 ]]; then
      warn "Node.js 22+ required for frontend. Found v${NODE_VER}. Skipping frontend."
      SKIP_FRONTEND=true
    else
      info "Node.js v${NODE_VER} ✓"
    fi
  fi
fi

# ── pre-flight: PostgreSQL ────────────────────────────────────────────────────
if [[ "$SKIP_INFRA_CHECK" == false ]]; then
  step "Checking infrastructure..."

  # Determine psql binary
  PSQL=""
  if command -v psql >/dev/null 2>&1; then
    PSQL="psql"
  else
    # Homebrew paths
    for candidate in \
      /opt/homebrew/opt/postgresql@15/bin/psql \
      /opt/homebrew/opt/postgresql@16/bin/psql \
      /usr/local/bin/psql; do
      if [[ -x "$candidate" ]]; then PSQL="$candidate"; break; fi
    done
  fi

  if [[ -z "$PSQL" ]]; then
    warn "psql not found — cannot verify PostgreSQL. Install PostgreSQL 13+ or pass --skip-infra-check."
  else
    # Version check
    PG_VER=$("$PSQL" -U postgres -t -c "SHOW server_version_num;" 2>/dev/null | tr -d ' \n' || echo "0")
    if [[ "$PG_VER" == "0" ]]; then
      error "Cannot connect to PostgreSQL as user 'postgres'."
      error "Make sure PostgreSQL is running and the 'postgres' user password is 'postgres'."
      error "Skip this check with --skip-infra-check if using Docker for the DB."
      exit 1
    fi

    PG_MAJOR=$(( PG_VER / 10000 ))
    if [[ "$PG_MAJOR" -lt 13 ]]; then
      error "PostgreSQL 13+ required. Found version $PG_MAJOR."
      exit 1
    fi
    info "PostgreSQL $PG_MAJOR ✓"

    # Check all 12 required databases exist
    REQUIRED_DBS=(
      event_db registration_db venue_db attendance_db ticket_db notification_db
      certificate_db feedback_db leaderboard_db announcement_db resource_db sponsor_db
    )
    MISSING_DBS=()
    for db in "${REQUIRED_DBS[@]}"; do
      exists=$("$PSQL" -U postgres -t -c "SELECT 1 FROM pg_database WHERE datname='${db}';" 2>/dev/null | tr -d ' \n')
      if [[ "$exists" != "1" ]]; then
        MISSING_DBS+=("$db")
      fi
    done

    if [[ ${#MISSING_DBS[@]} -gt 0 ]]; then
      error "Missing databases: ${MISSING_DBS[*]}"
      error "Create them with:"
      echo ""
      for db in "${MISSING_DBS[@]}"; do
        echo "    psql -U postgres -c \"CREATE DATABASE $db;\""
      done
      echo ""
      error "Or create all at once:"
      echo ""
      echo "    for db in ${REQUIRED_DBS[*]}; do psql -U postgres -c \"CREATE DATABASE \\\$db;\"; done"
      echo ""
      exit 1
    fi
    info "All 12 databases present ✓"
  fi

  # ── pre-flight: RabbitMQ ────────────────────────────────────────────────────
  if command -v nc >/dev/null 2>&1; then
    if ! nc -z localhost 5672 2>/dev/null; then
      error "RabbitMQ is not reachable on localhost:5672."
      error "Start it with: brew services start rabbitmq  (or rabbitmq-server -detached)"
      error "Or run via Docker: docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3.12-management"
      error "Skip this check with --skip-infra-check."
      exit 1
    fi
    info "RabbitMQ :5672 ✓"
  else
    warn "nc not found — skipping RabbitMQ port check."
  fi
fi

# ── build ─────────────────────────────────────────────────────────────────────
if [[ "$SKIP_BUILD" == false ]]; then
  step "Building all Java services (mvn clean package -DskipTests)..."
  cd "$ROOT"
  mvn clean package -DskipTests -q
  info "Build complete."
else
  warn "--skip-build: using existing JARs."
fi

# ── create log dir and clear old PIDs ────────────────────────────────────────
mkdir -p "$LOG_DIR"
rm -f "$PID_FILE"

# ── start Java services ───────────────────────────────────────────────────────
step "Starting Java services (delay: ${STARTUP_DELAY}s between each)..."
echo ""

for svc in "${JAVA_SERVICES[@]}"; do
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

# ── start frontend ────────────────────────────────────────────────────────────
if [[ "$SKIP_FRONTEND" == false ]]; then
  FRONTEND_DIR="$ROOT/frontend"
  if [[ ! -d "$FRONTEND_DIR" ]]; then
    warn "frontend/ directory not found — skipping."
  else
    step "Starting React frontend (Vite dev server)..."

    if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
      info "Running npm install in frontend/..."
      npm --prefix "$FRONTEND_DIR" install --silent
    fi

    LOG_FILE="$LOG_DIR/frontend.log"
    npm --prefix "$FRONTEND_DIR" run dev > "$LOG_FILE" 2>&1 &
    FE_PID=$!
    echo "frontend:${FE_PID}" >> "$PID_FILE"
    info "Started frontend  (PID $FE_PID) → logs/frontend.log"
  fi
fi

# ── summary ───────────────────────────────────────────────────────────────────
TOTAL=$(wc -l < "$PID_FILE" 2>/dev/null | tr -d ' ' || echo 0)
echo ""
info "All $TOTAL services started."
echo ""
echo "  Access points:"
if [[ "$SKIP_FRONTEND" == false ]]; then
  echo "    Frontend      →  http://localhost:3000"
fi
echo "    API Gateway   →  http://localhost:4069"
echo "    Eureka UI     →  http://localhost:4070"
echo "    RabbitMQ UI   →  http://localhost:15672  (guest / guest)"
echo ""
echo "  Commands:"
echo "    ./start-all.sh --stop                    stop all services"
echo "    ./start-all.sh --stop <name>             stop one service"
echo "    ./start-all.sh --restart <name>          restart one service"
echo "    ./start-all.sh --status                  show running/stopped status"
echo "    ./start-all.sh --logs                    tail all logs"
echo "    ./start-all.sh --logs <name>             tail one service log"
echo "    ./start-all.sh --skip-build              restart without rebuilding JARs"
echo "    ./start-all.sh --skip-infra-check        skip PostgreSQL/RabbitMQ checks"
echo "    ./start-all.sh --skip-frontend           skip the React dev server"
echo ""

# ── optional log tailing ─────────────────────────────────────────────────────
if [[ "$TAIL_LOGS" == true ]]; then
  info "Tailing all logs (Ctrl+C to stop tailing — services keep running)..."
  tail -f "$LOG_DIR"/*.log
fi
