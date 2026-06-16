#!/bin/sh
set -e

build_spring_datasource_url() {
  if [ -n "$DB_HOST" ] && [ -n "$DB_NAME" ]; then
    port="${DB_PORT:-5432}"
    printf 'jdbc:postgresql://%s:%s/%s?sslmode=require' "$DB_HOST" "$port" "$DB_NAME"
    return
  fi

  if [ -z "$DB_URL" ]; then
    return 1
  fi

  url="$DB_URL"
  case "$url" in
    jdbc:postgresql://*|jdbc:postgres://*) ;;
    postgresql://*) url="jdbc:$url" ;;
    postgres://*) url="jdbc:postgresql:${url#postgres:}" ;;
    *) return 1 ;;
  esac

  case "$url" in
    *@[^:]*/*)
      url="$(printf '%s' "$url" | sed -E 's@(://[^@]+@[^:/]+)/@\1:5432/@')"
      ;;
  esac

  case "$url" in
    *\?*) printf '%s' "${url}&sslmode=require" ;;
    *) printf '%s' "${url}?sslmode=require" ;;
  esac
}

if jdbc_url="$(build_spring_datasource_url)"; then
  export SPRING_DATASOURCE_URL="$jdbc_url"
  echo "Database configured for host=${DB_HOST:-from-connection-string}"
else
  echo "ERROR: missing database configuration (DB_HOST/DB_NAME or DB_URL)" >&2
  exit 1
fi

exec java -jar /app/app.jar
