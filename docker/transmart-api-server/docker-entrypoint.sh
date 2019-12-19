#!/bin/sh
set -e

# Error message and exit for missing environment variable
fatal() {
		cat << EndOfMessage
###############################################################################
!!!!!!!!!! FATAL ERROR !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
###############################################################################
			The variable with the name '$1' is unset.
			Please specify a value in this container environment using
			-e in docker run or the environment section in Docker Compose.
###############################################################################
EndOfMessage
		exit 1
}

# Check that Postgres and Keycloak Host are configured
# via environment variables
[[ ! -z ${PGHOST+x} ]] || fatal 'PGHOST'
[[ ! -z ${KEYCLOAK_SERVER_URL+x} ]] || fatal 'KEYCLOAK_SERVER_URL'
[[ ! -z ${KEYCLOAK_REALM+x} ]] || fatal 'KEYCLOAK_REALM'
[[ ! -z ${KEYCLOAK_CLIENT_ID+x} ]] || fatal 'KEYCLOAK_CLIENT_ID'

# Fixed values, not configurable by user
APP_PORT=8081
TRANSMART_API_SERVER_CONFIG_FILE="${TRANSMART_USER_HOME}/transmart-api-server.config.yml"

CERTS_PATH="${TRANSMART_USER_HOME}/extra_certs.pem"

cat > "${TRANSMART_API_SERVER_CONFIG_FILE}" <<EndOfMessage
---
# Database configuration
dataSource:
    driverClassName: org.postgresql.Driver
    dialect: org.hibernate.dialect.PostgreSQLDialect
    url: jdbc:postgresql://${PGHOST}:${PGPORT:-5432}/${PGDATABASE:-transmart}?currentSchema=public
    username: biomart_user
    password: ${BIOMART_USER_PASSWORD:-biomart_user}

# Create or update the database schema at application startup
grails.plugin.databasemigration.updateOnStart: true

# Disable saving application logs in the database
org.transmartproject.system.writeLogToDatabase: false

# If true, deny access to users without any role
org.transmartproject.security.denyAccessToUsersWithoutRole: ${DENY_ACCESS_WITHOUT_ROLE:-false}

# Keycloak configuration
keycloak:
    realm: ${KEYCLOAK_REALM}
    bearer-only: true
    auth-server-url: ${KEYCLOAK_SERVER_URL}/auth
    resource: ${KEYCLOAK_CLIENT_ID}
    use-resource-role-mappings: true
    verify-token-audience: true
EndOfMessage
sync

[[ -f "${CERTS_PATH}" ]] && \
  keytool -import -trustcacerts -file "${CERTS_PATH}" -alias certificate-alias -keystore "${TRANSMART_USER_HOME}/cacerts" -storepass password -noprompt && \
  mv "${TRANSMART_USER_HOME}/cacerts" /etc/ssl/certs/java/cacerts

unset PGHOST
unset PGPORT
unset PGDATABASE
unset KEYCLOAK_SERVER_URL
unset KEYCLOAK_REALM
unset KEYCLOAK_CLIENT_ID
unset BIOMART_USER_PASSWORD
unset TRANSMART_USER_NAME
unset TRANSMART_GROUP_NAME
unset TRANSMART_USER_HOME
unset TRANSMART_API_SERVER_WAR_URL

exec java -jar -server \
          "-Djava.awt.headless=true" \
          "-Dserver.port=${APP_PORT}" \
          "-Djava.security.egd=file:///dev/urandom" \
          "-Dspring.config.location=${TRANSMART_API_SERVER_CONFIG_FILE}" \
          "-Dlogging.config=/logback.groovy" \
					"${TRANSMART_SERVICE_WAR_FILE}"
