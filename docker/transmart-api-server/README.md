# TranSMART API Server Docker Image

## Configuration

The image requires the following environment variables to be present:

NAME            | Description
----------------|--------------------------------------------------------
`PGHOST`        | Where the Postgres database is hosted
`PGPORT`        | Port that the API server should use to contact Postgres
`KEYCLOAK_HOST` | Where Keycloak is hosted
`KEYCLOAK_PORT` | Port that the API server should use to contact Keycloak

The values are set in the config.yml file in the entrypoint of the Docker image,
represented by the `docker-entrypoint.sh` file.

If a certificate file is mounted as volume at `/home/transmart/extra_certs.pem`,
the file is imported to the default certificate store of Java at startup. 

## Ports

The image exposes the following ports:

Value    | Type  | Description
---------|-------|-----------------
8081     | `tcp` | Port of the API Server  


## Volumes
None
