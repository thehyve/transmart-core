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
[ ! -z ${PGHOST+x} ] || fatal 'PGHOST'
[ ! -z ${PGPORT+x} ] || fatal 'PGPORT'
[ ! -z ${KEYCLOAK_HOST+x} ] || fatal 'KEYCLOAK_HOST'
[ ! -z ${KEYCLOAK_PORT+x} ] || fatal 'KEYCLOAK_PORT'

# Fixed values, not configurable by user
PGDATABASE=transmart
BIOMART_USER='biomart_user'
BIOMART_PASSWORD="${BIOMART_USER}"
TRANSMART_API_SERVER_CONFIG_FILE="${TRANSMART_USER_HOME}/transmart-api-server.config.yml"

cat > "${TRANSMART_API_SERVER_CONFIG_FILE}" <<EndOfMessage
---
grails:
    profile: rest-api
    codegen:
        defaultPackage: org.transmartproject.api.server
    spring:
        transactionManagement:
            proxies: false
info:
    app:
        name: '@info.app.name@'
        version: '@info.app.version@'
        grailsVersion: '@info.app.grailsVersion@'
spring:
    main:
        banner-mode: "off"
    groovy:
        template:
            check-template-location: false

# Spring Actuator Endpoints are Disabled by Default
endpoints:
    enabled: false
    jmx:
        enabled: true

---
hibernate:
    cache:
        queries: false
        use_second_level_cache: true
        use_query_cache: false
        region.factory_class: org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory

dataSource:
    url: jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
    driverClassName: org.postgresql.Driver
    username: ${BIOMART_USER}
    password: ${BIOMART_PASSWORD}
    dbCreate: none
    pooled: true
    jmxExport: true
    logSql: false
    formatql: false
    properties:
        jmxEnabled: true
        initialSize: 5
        maxActive: 50
        minIdle: 5
        maxIdle: 25
        maxWait: 10000
        maxAge: 600000
        timeBetweenEvictionRunsMillis: 5000
        minEvictableIdleTimeMillis: 60000
        validationQuery: SELECT 1
        validationQueryTimeout: 3
        validationInterval: 15000
        testOnBorrow: true
        testWhileIdle: true
        testOnReturn: false
        jdbcInterceptors: ConnectionState
        defaultTransactionIsolation: 2 # TRANSACTION_READ_COMMITTED

grails:
    cors:
        enabled: true

server:
    port: 8081

environments:
    test:
        keycloak:
            realm: test
            bearer-only: true
            auth-server-url: http://localhost:8080/auth
            resource: transmart
            use-resource-role-mappings: true
    development:
        keycloak:
            realm: dev
            bearer-only: true
            auth-server-url: http://${KEYCLOAK_HOST}:${KEYCLOAK_PORT}/auth
            resource: transmart
            use-resource-role-mappings: true
#            disable-trust-manager: false  # when true, SSL certificate checking is disabled. Do not use that in production!

#        keycloakOffline:
#            offlineToken: <offline token>
EndOfMessage
sync

# Wait for services to come up before trying to start the API server
dockerize -wait "tcp://${PGHOST}:${PGPORT}"

unset PGHOST
unset PGPORT
unset PGDATABASE
unset KEYCLOAK_HOST
unset KEYCLOAK_PORT
unset BIOMART_USER
unset BIOMART_PASSWORD
unset TRANSMART_USER_NAME
unset TRANSMART_GROUP_NAME
unset TRANSMART_USER_HOME
unset TRANSMART_API_SERVER_WAR_URL
unset DOCKERIZE_VERSION

exec java -jar \
          "-Dspring.config.location=${TRANSMART_API_SERVER_CONFIG_FILE}" \
					"${TRANSMART_SERVICE_WAR_FILE}"
