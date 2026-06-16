#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

# Render static site: app chạy ở root domain (không dùng /sit-ui prefix).
# PUBLIC_URL phải là "." — chuỗi rỗng bị CRA coi là falsy và fallback về homepage "/sit-ui".
export PUBLIC_URL=.
export REACT_APP_BASENAME=

if [ -n "$SIT_API_SERVICE_URL" ]; then
  export REACT_APP_API_URL="${SIT_API_SERVICE_URL}/sit-api"
else
  echo "WARN: SIT_API_SERVICE_URL chưa set — dùng localhost (chỉ khi build local)"
  export REACT_APP_API_URL="http://localhost:8001/sit-api"
fi

echo "Building sit-ui with REACT_APP_API_URL=$REACT_APP_API_URL"

npm ci
npm run build
