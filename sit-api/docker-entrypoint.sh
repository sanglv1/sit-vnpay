#!/bin/sh
set -e

if [ -n "$DB_URL" ]; then
  case "$DB_URL" in
    jdbc:*) ;;
    postgresql:*|postgres:*)
      export DB_URL="jdbc:$DB_URL"
      ;;
  esac
fi

exec java -jar /app/app.jar
