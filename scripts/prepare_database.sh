#!/usr/bin/env bash

schemas=$(dirname "${0}")/../transmart-schemas
version=$(cat "${schemas}/VERSION")
jar="${schemas}/build/libs/transmart-schemas-${version}.jar"

# Check if the jar present
if [[ ! -e "${jar}" ]]; then
    echo "Cannot find jar: ${jar}"
    echo "To build it, run:"
    echo "  gradle assemble"
    exit 1
fi

# Choose the location of the java binary
prefix=
if [[ ! "x${JAVA_HOME}" == "x" ]]; then
    prefix="${JAVA_HOME}/bin/"
fi

if [[ "x${PGDATABASE}" == "x" ]]; then
    PGDATABASE=transmart
fi
if [[ "x${PGPORT}" == "x" ]]; then
    PGPORT=5432
fi
if [[ "x${PGUSER}" == "x" ]]; then
    PGUSER=biomart_user
fi
if [[ "x${PGPASSWORD}" == "x" ]]; then
    PGPASSWORD=biomart_user
fi

echo "Preparing database ${PGDATABASE} for ${PGUSER} ..."

if [[ "$(sudo -u postgres psql -At -c "select rolname from pg_roles where rolname = '${PGUSER}';")" != "${PGUSER}" ]]; then
  sudo -u postgres psql -c "create role ${PGUSER} with password '${PGPASSWORD}' login;"
fi
if [[ "$(sudo -u postgres psql -At -c "select datname from pg_database where datname = '${PGDATABASE}';")" != "${PGDATABASE}" ]]; then
  sudo -u postgres psql -c "create database \"${PGDATABASE}\";"
  sudo -u postgres psql -c "grant all on database \"${PGDATABASE}\" to ${PGUSER};"
fi
"${prefix}java" -jar "-Ddb_name=${PGDATABASE}" "-Ddb_port=${PGPORT}" "-Ddb_username=${PGUSER}" "-Dspring.datasource.password=${PGPASSWORD}" "${jar}" || {
    echo "Database creation or update failed. Maybe the database was created with another tool?"
    exit 1
}
echo "Database prepared."
