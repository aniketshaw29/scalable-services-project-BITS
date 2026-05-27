#!/usr/bin/env bash
# test-all.sh — run tests for every Campus EventHub service
# Run from the project root: ./test-all.sh
#
# Options:
#   --fail-fast    stop on first service failure
#   --service <n>  run tests for one service only (e.g. --service event-service)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

# ── colour helpers ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; BOLD='\033[1m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
header()  { echo -e "\n${BOLD}$*${NC}"; }

# ── service list ─────────────────────────────────────────────────────────────
ALL_SERVICES=(
  "eureka-server"
  "api-gateway"
  "event-service"
  "registration-service"
  "venue-service"
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

# ── parse flags ───────────────────────────────────────────────────────────────
FAIL_FAST=false
SINGLE_SERVICE=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --fail-fast)   FAIL_FAST=true ;;
    --service)     SINGLE_SERVICE="${2:-}"; shift ;;
    *) warn "Unknown option: $1" ;;
  esac
  shift
done

if [[ -n "$SINGLE_SERVICE" ]]; then
  SERVICES=("$SINGLE_SERVICE")
else
  SERVICES=("${ALL_SERVICES[@]}")
fi

# ── pre-flight ────────────────────────────────────────────────────────────────
command -v java >/dev/null 2>&1 || { error "java not found."; exit 1; }
command -v mvn  >/dev/null 2>&1 || { error "mvn not found."; exit 1; }

# ── run tests ─────────────────────────────────────────────────────────────────
PASSED=()
FAILED=()
START_TIME=$(date +%s)

header "Running tests for ${#SERVICES[@]} service(s)..."
echo ""

for svc in "${SERVICES[@]}"; do
  SVC_DIR="$ROOT/$svc"
  if [[ ! -d "$SVC_DIR" ]]; then
    warn "Directory not found: $SVC_DIR — skipping."
    continue
  fi

  echo -e "${BOLD}── $svc${NC}"
  SVC_START=$(date +%s)

  if mvn -f "$SVC_DIR/pom.xml" test -q 2>&1; then
    SVC_END=$(date +%s)
    elapsed=$(( SVC_END - SVC_START ))
    info "PASS  $svc  (${elapsed}s)"
    PASSED+=("$svc")
  else
    SVC_END=$(date +%s)
    elapsed=$(( SVC_END - SVC_START ))
    error "FAIL  $svc  (${elapsed}s)"
    FAILED+=("$svc")
    if [[ "$FAIL_FAST" == true ]]; then
      echo ""
      error "Stopping early (--fail-fast)."
      break
    fi
  fi
  echo ""
done

# ── summary ───────────────────────────────────────────────────────────────────
END_TIME=$(date +%s)
TOTAL=$(( END_TIME - START_TIME ))

header "─────────────────────────────────────────"
header "  Test Results"
header "─────────────────────────────────────────"
echo ""

if [[ ${#PASSED[@]} -gt 0 ]]; then
  echo -e "${GREEN}  PASSED (${#PASSED[@]})${NC}"
  for s in "${PASSED[@]}"; do echo "    ✓  $s"; done
  echo ""
fi

if [[ ${#FAILED[@]} -gt 0 ]]; then
  echo -e "${RED}  FAILED (${#FAILED[@]})${NC}"
  for s in "${FAILED[@]}"; do echo "    ✗  $s"; done
  echo ""
fi

echo "  Total time: ${TOTAL}s"
echo ""

if [[ ${#FAILED[@]} -gt 0 ]]; then
  error "Some tests failed. Re-run a single service with:"
  echo "    ./test-all.sh --service <service-name>"
  echo ""
  echo "  Or view the Surefire report:"
  echo "    open <service>/target/surefire-reports/*.txt"
  echo ""
  exit 1
else
  info "All tests passed."
  exit 0
fi
