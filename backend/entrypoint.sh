#!/bin/sh
# Normalizes Render-style postgres URLs for the Postgres JDBC driver.

set -eu

normalize_pg_url() {
  # $1 = url var name, $2 = username var name, $3 = password var name
  url_var="$1"
  user_var="$2"
  pass_var="$3"

  eval "url=\${$url_var:-}"
  if [ -z "${url:-}" ]; then
    return 0
  fi

  # Already JDBC
  case "$url" in
    jdbc:*) return 0 ;;
  esac

  # Only handle postgres-like schemes.
  case "$url" in
    postgresql://*|postgres://*) ;;
    *) return 0 ;;
  esac

  rest="${url#*://}"

  # Split off userinfo at the LAST '@'.
  case "$rest" in
    *@*)
      userinfo="${rest%@*}"
      hostdb="${rest##*@}"
      dbuser="${userinfo%%:*}"
      case "$userinfo" in
        *:*) dbpass="${userinfo#*:}" ;;
        *) dbpass="" ;;
      esac
      eval "cur_user=\${$user_var:-}"
      if [ -z "${cur_user:-}" ] && [ -n "${dbuser:-}" ]; then
        eval "$user_var=\"\$dbuser\""
        eval "export $user_var"
      fi
      eval "cur_pass=\${$pass_var:-}"
      if [ -z "${cur_pass:-}" ] && [ -n "${dbpass:-}" ]; then
        eval "$pass_var=\"\$dbpass\""
        eval "export $pass_var"
      fi
      ;;
    *)
      hostdb="$rest"
      ;;
  esac

  new_url="jdbc:postgresql://$hostdb"
  # Render external databases require SSL.
  case "$new_url" in
    *\?*) ;;
    *) new_url="$new_url?sslmode=require" ;;
  esac
  eval "$url_var=\"\$new_url\""
  eval "export $url_var"
}

normalize_pg_url "SPRING_DATASOURCE_URL" "SPRING_DATASOURCE_USERNAME" "SPRING_DATASOURCE_PASSWORD"
normalize_pg_url "DATABASE_URL" "DATABASE_USERNAME" "DATABASE_PASSWORD"

exec java ${JAVA_OPTS:-} -jar app.jar
