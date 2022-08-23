# Docker scripts for the TranSMART API server and database

[![Docker Build Status](https://img.shields.io/docker/pulls/thehyve/transmart-api-server.svg)](https://hub.docker.com/r/thehyve/transmart-api-server)

## Run

Install [docker-compose](https://docs.docker.com/compose/install/) and run
```bash
docker-compose up
```

This starts a PostgreSQL database and the TranSMART API server, which will
create minimal database schemas, see [transmart-schemas](../transmart-schemas).

The application uses Keycloak for authentication. The following environment variables
can be used to configure Keycloak:

Variable              | Default value
----------------------|---------------
`KEYCLOAK_SERVER_URL` | https://keycloak-dwh-test.thehyve.net/auth
`KEYCLOAK_REALM`      | transmart-dev
`KEYCLOAK_CLIENT_ID`  | transmart-client

### Ports

The image exposes the following ports:

Value    | Type  | Description
---------|-------|-----------------
9081     | `tcp` | The TranSMART API Server
9432     | `tcp` | PostgreSQL database server

Connect to the database using:
```bash
psql -h localhost -p 9432 -U biomart_user
```
The application is available at http://localhost:9081.


## Development

### Build

```bash
TRANSMART_VERSION=$(gradle properties | grep '^version: ' - | awk '{print $2}')
docker build -t transmart-api-server transmart-api-server
```

### Publish

Publish the image to [Docker Hub](https://hub.docker.com/r/thehyve/transmart-api-server):

```bash
docker login
TRANSMART_VERSION="17.2.14"
docker tag transmart-api-server "thehyve/transmart-api-server:${TRANSMART_VERSION}"
docker push "thehyve/transmart-api-server:${TRANSMART_VERSION}"
```
