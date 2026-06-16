#!/bin/sh
set -e

db_region() {
  printf '%s' "${DB_REGION:-${RENDER_REGION:-oregon}}"
}

resolve_pg_host() {
  host="$1"
  case "$host" in
    *.*) printf '%s' "$host" ;;
    *)
      region="$(db_region)"
      printf '%s.%s-postgres.render.com' "$host" "$region"
      ;;
  esac
}

fix_short_hostname_in_url() {
  url="$1"
  region="$(db_region)"
  printf '%s' "$url" | sed -E "s@(://[^@]+@)([^:/@.]+)(:|/)@\1\2.${region}-postgres.render.com\3@"
}

build_spring_datasource_url() {
  if [ -n "$DB_HOST" ] && [ -n "$DB_NAME" ]; then
    host="$(resolve_pg_host "$DB_HOST")"
    port="${DB_PORT:-5432}"
    printf 'jdbc:postgresql://%s:%s/%s?sslmode=require' "$host" "$port" "$DB_NAME"
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

  url="$(fix_short_hostname_in_url "$url")"

  case "$url" in
    *\?*) printf '%s' "${url}&sslmode=require" ;;
    *) printf '%s' "${url}?sslmode=require" ;;
  esac
}

if jdbc_url="$(build_spring_datasource_url)"; then
  export SPRING_DATASOURCE_URL="$jdbc_url"
  db_host="$(printf '%s' "$jdbc_url" | sed -E 's#.*@([^:/?]+).*#\1#')"
  echo "Database configured (region=$(db_region), host=${db_host})"
else
  echo "ERROR: missing database configuration (DB_HOST/DB_NAME or DB_URL)" >&2
  exit 1
fi

exec java -jar /app/app.jar
