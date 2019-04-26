#!/usr/bin/env bash

here=$(dirname "${0}")
version=$(cat "${here}/VERSION")
jar="${here}/build/libs/transmart-schemas-${version}.jar"

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
    PGDATABASE=transmartDev
fi
if [[ "x${TM_CZ_PWD}" == "x" ]]; then
    TM_CZ_PWD=tm_cz
fi
if [[ "x${BIOMART_USER_PWD}" == "x" ]]; then
    BIOMART_USER_PWD=biomart_user
fi

if [[ "$(sudo -u postgres psql -At -c "select rolname from pg_roles where rolname = 'tm_cz';")" != "tm_cz" ]]; then
  sudo -u postgres psql -c "create role tm_cz with password '${TM_CZ_PWD}';"
fi
if [[ "$(sudo -u postgres psql -At -c "select rolname from pg_roles where rolname = 'biomart_user';")" != "biomart_user" ]]; then
  sudo -u postgres psql -c "create role biomart_user with password '${BIOMART_USER_PWD}';"
fi
if [[ "$(sudo -u postgres psql -At -c "select datname from pg_database where datname = '${PGDATABASE}';")" != "${PGDATABASE}" ]]; then
  sudo -u postgres psql -c "create database \"${PGDATABASE}\";"
  sudo -u postgres psql -c "grant all on database \"${PGDATABASE}\" to tm_admin;"
fi
${prefix}java -jar -Dspring.config.location=./database.yml "${jar}" || {
    echo "Database creation or update failed. Maybe the database was created with another tool?"
    exit 1
}
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on schema i2b2demodata to biomart_user;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on schema i2b2metadata to biomart_user;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on all tables in schema i2b2demodata to biomart_user;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on all tables in schema i2b2metadata to biomart_user;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on schema i2b2demodata to tm_cz;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on schema i2b2metadata to tm_cz;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on all tables in schema i2b2demodata to tm_cz;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on all tables in schema i2b2metadata to tm_cz;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on all sequences in schema i2b2demodata to tm_cz;'
sudo -u postgres psql -d "${PGDATABASE}" -c 'grant all on all sequences in schema i2b2metadata to tm_cz;'
