#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

load_env_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%%#*}"
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    line="${line//$'\r'/}"
    [[ -z "$line" ]] && continue
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      local key="${BASH_REMATCH[1]}"
      local val="${BASH_REMATCH[2]}"
      if [[ "$val" =~ ^\".*\"$ ]]; then
        val="${val:1:${#val}-2}"
      elif [[ "$val" =~ ^\'.*\'$ ]]; then
        val="${val:1:${#val}-2}"
      fi
      export "${key}=${val}"
    fi
  done < "$file"
}

echo "=== .env (shell) ==="
load_env_file .env
echo "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-<unset>}"
echo "DB_URL=${DB_URL:-<unset>}"
echo "DB_USERNAME=${DB_USERNAME:-<unset>}"

PID=$(pgrep -f 'sit-api.*\.jar' | head -1 || true)
if [[ -n "$PID" ]]; then
  echo ""
  echo "=== sit-api process (PID $PID) ==="
  sudo tr '\0' '\n' < "/proc/$PID/environ" | grep -E 'SPRING_PROFILES|^(DB_|SPRING_DATASOURCE)' | sort || true
else
  echo "WARN: sit-api jar process not found"
fi

echo ""
echo "=== PostgreSQL sit_user (peer auth) ==="
sudo -u postgres psql -d sit_vnpay_db -c \
  "SELECT id, email, role, active, LEFT(password_hash,7) AS h, LENGTH(password_hash) AS len FROM sit_user;"

if [[ -n "${DB_PASSWORD:-}" ]]; then
  echo ""
  echo "=== PostgreSQL sit_user (TCP / JDBC auth) ==="
  PGPASSWORD="$DB_PASSWORD" psql -h 127.0.0.1 -p 5432 -U "${DB_USERNAME:-postgres}" -d sit_vnpay_db -c \
    "SELECT COUNT(*) AS cnt FROM sit_user;" || echo "TCP auth FAILED"
fi

echo ""
echo "=== login API ==="
curl -s -X POST http://127.0.0.1:8001/sit-api/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sanglv@vnpay.vn","password":"Cttvnpay@2026"}'
echo ""
