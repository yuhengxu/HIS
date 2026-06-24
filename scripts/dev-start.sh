#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
ADMIN_DIR="$ROOT_DIR/frontend/admin"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$RUN_DIR/logs"
BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/admin-web.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/admin-web.log"
BACKEND_PORT="${BACKEND_PORT:-18080}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-15173}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
REDIS_PORT="${REDIS_PORT:-6379}"
PUBLIC_HOST="${PUBLIC_HOST:-82.156.67.222}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-90}"

ACTION="start"
START_INFRA=1
STOP_INFRA=0
SKIP_INSTALL=0

usage() {
  cat <<'HELP'
Usage: scripts/dev-start.sh [action] [options]

Actions:
  start      Start the local dev stack. This is the default action.
  stop       Stop backend and admin web processes recorded in .run/*.pid.
  restart    Stop recorded processes, then start the local dev stack.
  status     Show recorded process status and Docker Compose service status.

Options:
  --no-infra       Do not start PostgreSQL/Redis during start or restart.
  --stop-infra     Also stop PostgreSQL/Redis during stop or restart.
  --skip-install   Do not run npm install when frontend/admin/node_modules is missing.
  -h, --help       Show this help message.

Default ports:
  Backend:    http://localhost:18080   (BACKEND_PORT)
  Admin web:  http://localhost:15173   (ADMIN_WEB_PORT)
  PostgreSQL: localhost:5432           (POSTGRES_PORT)
  Redis:      localhost:6379           (REDIS_PORT)
  Public IP:  82.156.67.222            (PUBLIC_HOST)

Notes:
  - start runs backend/admin web in the background, writes logs to .run/logs,
    waits for readiness, prints SUCCESS, then exits.
  - The script checks ports before startup and exits if backend/admin ports are
    already in use.
  - PostgreSQL/Redis ports are allowed only when already used by this project's
    Docker Compose services.
  - The script clears proxy environment variables in its own process.
  - stop only stops processes recorded by this script; it does not kill unrelated
    processes that happen to use the same ports.
HELP
}

for arg in "$@"; do
  case "$arg" in
    start|stop|restart|status)
      ACTION="$arg"
      ;;
    --no-infra)
      START_INFRA=0
      ;;
    --stop-infra)
      STOP_INFRA=1
      ;;
    --skip-install)
      SKIP_INSTALL=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option or action: $arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

unset http_proxy https_proxy all_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

port_in_use() {
  local port="$1"
  ss -ltn "sport = :$port" 2>/dev/null | awk 'NR > 1 { found=1 } END { exit found ? 0 : 1 }'
}

describe_port() {
  local port="$1"
  ss -ltnp "sport = :$port" 2>/dev/null || ss -ltn "sport = :$port" 2>/dev/null || true
}

compose_service_running() {
  local service="$1"
  (cd "$ROOT_DIR" && docker compose ps --status running --services "$service" 2>/dev/null) | grep -qx "$service"
}

assert_port_free() {
  local port="$1"
  local label="$2"
  if port_in_use "$port"; then
    echo "Port $port is already in use; cannot start $label." >&2
    describe_port "$port" >&2
    echo "Stop the existing process or change its port before rerunning this script." >&2
    exit 1
  fi
}

assert_infra_port_available() {
  local port="$1"
  local label="$2"
  local service="$3"
  if port_in_use "$port"; then
    if compose_service_running "$service"; then
      echo "$label port $port is already used by this project's Docker Compose service; reusing it."
      return
    fi
    echo "Port $port is already in use; cannot start $label." >&2
    describe_port "$port" >&2
    echo "Stop the existing service, change $label port, or use --no-infra if infrastructure is already managed elsewhere." >&2
    exit 1
  fi
}

check_docker_access() {
  require_command docker
  if docker ps >/dev/null 2>&1; then
    return
  fi

  echo "Cannot access Docker daemon as user '$USER'." >&2
  if id -nG "$USER" 2>/dev/null | tr ' ' '\n' | grep -qx docker; then
    echo "The user is in the docker group, but this shell has not refreshed group membership." >&2
    echo "Run 'newgrp docker' in this terminal, or log out and log in again, then rerun this script." >&2
  else
    echo "Add the user to the docker group first:" >&2
    echo "  sudo usermod -aG docker $USER" >&2
    echo "Then log out and log in again, or run 'newgrp docker'." >&2
  fi
  echo "Immediate workaround: run the infrastructure command with sudo, then start with --no-infra:" >&2
  echo "  sudo docker compose up -d postgres redis" >&2
  echo "  scripts/dev-start.sh start --no-infra" >&2
  exit 1
}

pid_alive() {
  local pid="$1"
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

read_pid() {
  local file="$1"
  if [ -f "$file" ]; then
    tr -d '[:space:]' < "$file"
  fi
}

status_one() {
  local name="$1"
  local file="$2"
  local pid
  pid="$(read_pid "$file")"
  if pid_alive "$pid"; then
    echo "$name: running (pid $pid)"
  elif [ -n "$pid" ]; then
    echo "$name: stopped (stale pid $pid)"
  else
    echo "$name: stopped"
  fi
}

stop_one() {
  local name="$1"
  local file="$2"
  local pid
  pid="$(read_pid "$file")"
  if pid_alive "$pid"; then
    echo "Stopping $name (pid $pid)..."
    kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
    for _ in $(seq 1 20); do
      if ! pid_alive "$pid"; then
        break
      fi
      sleep 0.2
    done
    if pid_alive "$pid"; then
      echo "Force stopping $name (pid $pid)..."
      kill -9 -- "-$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
    fi
  elif [ -n "$pid" ]; then
    echo "$name is not running (stale pid $pid)."
  else
    echo "$name is not running."
  fi
  rm -f "$file"
}

stop_stack() {
  stop_one "admin web" "$FRONTEND_PID_FILE"
  stop_one "backend" "$BACKEND_PID_FILE"
  if [ "$STOP_INFRA" -eq 1 ]; then
    check_docker_access
    echo "Stopping PostgreSQL and Redis..."
    (cd "$ROOT_DIR" && docker compose stop postgres redis)
  fi
}

show_status() {
  status_one "backend" "$BACKEND_PID_FILE"
  status_one "admin web" "$FRONTEND_PID_FILE"
  echo "Backend log:   $BACKEND_LOG"
  echo "Admin web log: $FRONTEND_LOG"
  if command -v docker >/dev/null 2>&1; then
    echo
    echo "Docker Compose services:"
    (cd "$ROOT_DIR" && docker compose ps postgres redis 2>/dev/null || true)
  fi
}

wait_for_url() {
  local label="$1"
  local url="$2"
  local log_file="$3"
  local deadline=$((SECONDS + START_TIMEOUT_SECONDS))

  while [ "$SECONDS" -lt "$deadline" ]; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "$label did not become ready within ${START_TIMEOUT_SECONDS}s." >&2
  echo "Log: $log_file" >&2
  tail -n 80 "$log_file" >&2 || true
  return 1
}

start_stack() {
  require_command java
  require_command mvn
  require_command node
  require_command npm
  require_command curl

  if [ "$START_INFRA" -eq 1 ]; then
    check_docker_access
    assert_infra_port_available "$POSTGRES_PORT" "PostgreSQL" "postgres"
    assert_infra_port_available "$REDIS_PORT" "Redis" "redis"
  fi

  assert_port_free "$BACKEND_PORT" "backend"
  assert_port_free "$ADMIN_WEB_PORT" "admin web"

  mkdir -p "$RUN_DIR" "$LOG_DIR"

  echo "HIS dev stack"
  echo "Root: $ROOT_DIR"
  echo "Java: $(java -version 2>&1 | head -n 1)"
  echo "Node: $(node -v)"
  echo "npm: $(npm -v)"

  if [ "$START_INFRA" -eq 1 ]; then
    echo "Starting PostgreSQL and Redis..."
    (cd "$ROOT_DIR" && docker compose up -d postgres redis)
  fi

  if [ "$SKIP_INSTALL" -eq 0 ] && [ ! -d "$ADMIN_DIR/node_modules" ]; then
    echo "Installing admin web dependencies..."
    (cd "$ADMIN_DIR" && npm install)
  fi

  : > "$BACKEND_LOG"
  : > "$FRONTEND_LOG"

  echo "Starting backend in background: http://localhost:$BACKEND_PORT"
  setsid bash -c "cd '$BACKEND_DIR' && exec mvn spring-boot:run -Dspring-boot.run.arguments='--server.address=0.0.0.0 --server.port=$BACKEND_PORT'" >>"$BACKEND_LOG" 2>&1 &
  BACKEND_PID=$!
  echo "$BACKEND_PID" > "$BACKEND_PID_FILE"

  echo "Starting admin web in background: http://localhost:$ADMIN_WEB_PORT"
  setsid bash -c "cd '$ADMIN_DIR' && BACKEND_PORT='$BACKEND_PORT' VITE_API_PROXY_TARGET='http://localhost:$BACKEND_PORT' exec ./node_modules/.bin/vite --host 0.0.0.0 --port '$ADMIN_WEB_PORT'" >>"$FRONTEND_LOG" 2>&1 &
  FRONTEND_PID=$!
  echo "$FRONTEND_PID" > "$FRONTEND_PID_FILE"

  if ! wait_for_url "Backend" "http://localhost:$BACKEND_PORT/api/v1/system/health" "$BACKEND_LOG"; then
    stop_stack
    exit 1
  fi

  if ! wait_for_url "Admin web" "http://localhost:$ADMIN_WEB_PORT" "$FRONTEND_LOG"; then
    stop_stack
    exit 1
  fi

  echo
  echo "SUCCESS: dev stack started."
  echo "Backend local:    http://localhost:$BACKEND_PORT"
  echo "Backend public:   http://$PUBLIC_HOST:$BACKEND_PORT"
  echo "Admin web local:  http://localhost:$ADMIN_WEB_PORT"
  echo "Admin web public: http://$PUBLIC_HOST:$ADMIN_WEB_PORT"
  echo "PID files: $RUN_DIR"
  echo "Logs: $LOG_DIR"
  echo "Stop: scripts/dev-start.sh stop"
}

case "$ACTION" in
  start)
    start_stack
    ;;
  stop)
    stop_stack
    ;;
  restart)
    stop_stack
    start_stack
    ;;
  status)
    show_status
    ;;
esac
