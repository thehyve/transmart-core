Dockerized version of the tranSMART server.
This container contains only the server, a database is needed for the server to run.
[See this README for more information.](../README.md)

# Environment Variables
* `ADMIN_PASSWORD`: password of user `admin` in the WildFly management interface (default: `admin`)
* `TRANSMART_URL`: externally exposed URL of transmart (default `http://localhost:8080/transmart-server`)
* `DB_HOST`: PostgreSQL database host (default: `transmart-database`)
* `DB_PORT`: PostgreSQL database port (default: `5432`)
* `DB_NAME`: PostgreSQL database name (default: `transmart`)
* `DB_USER`: PostgreSQL database user (default: `biomart_user`)
* `DB_USER`: PostgreSQL database password (default: `biomart_user`)
* `SOLR_HOST`: SOLR host (default: `solr`)
* `SOLR_PORT`: SOLR port (default: `8983`)

# Exposed interfaces
* `:8080/transmart-server/(v1|v2)`: API endpoint of transmart
* `:8080/transmart-server/`: tranSMARTApp Web UI
* `:9990/`: WildFly Management Interface

# Other information
* The Docker build context needs to be defined as the `docker` folder
