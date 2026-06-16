#!/bin/sh
set -e

append_ssl_mode() {
  url="$1"
  case "$url" in
    *\?*) printf '%s' "${url}&sslmode=require" ;;
    *) printf '%s' "${url}?sslmode=require" ;;
  esac
}

if [ -n "$DB_URL" ]; then
  case "$DB_URL" in
    jdbc:*)
      export DB_URL="$(append_ssl_mode "$DB_URL")"
      ;;
    postgresql:*|postgres:*)
      export DB_URL="$(append_ssl_mode "jdbc:$DB_URL")"
      ;;
  esac
fi

exec java -jar /app/app.jar
