# TranSMART API server Docker image

## Configuration

The image requires the following environment variables to be present:

Variable              | Default
----------------------|-------------
`PGHOST`              |
`PGPORT`              | `5432`
`PGDATABASE`          | `transmart`
`KEYCLOAK_SERVER_URL` |
`KEYCLOAK_REALM`      |
`KEYCLOAK_CLIENT_ID`  |

The values are set in the config.yml file in the entrypoint of the Docker image,
represented by the `docker-entrypoint.sh` file.

If a certificate file is mounted as volume at `/home/transmart/extra_certs.pem`,
the file is imported to the default certificate store of Java at startup. 

## Ports

The docker-compose script exposes the following ports:

Value    | Type  | Description
---------|-------|-----------------
8081     | `tcp` | Port of the API Server

## Volumes

- `transmart-db-data`
