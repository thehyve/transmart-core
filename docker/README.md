Docker image definitions and docker-compose profiles to run tranSMART and its dependencies.
`transmart-server` runs the tranSMART application server through WildFly, and `transmart-database` its database through PostgreSQL.

# Usage
In this directory, first initialise the database by running:
```
docker-compose -f docker-compose.xxx.yml up transmart-database
```

One the initialisation completed, stop it (with `Ctrl+C`), and run the full stack with:
```
docker-compose -f docker-compose.xxx.yml up
```

Those commands will build or download the images, and run the containers.
*Note*: you have to replace the `xxx` with the appropriate profile, see the section below.

# Docker-compose profiles
* `docker-compose.src.yml`: builds the different images from source, and starts `transmart-server`, `transmart-database` and `solr`
* `docker-compose.bin.yml`: downloads the different images from Docker Hub, and starts `transmart-server` and `transmart-database`

With those profiles, the ports exposed by the containers are shifted by +5, e.g. the transmart API endpoint will be `http://localhost:8085/transmart-server`.

# Docker Cloud Automated Builds
See the following links to check the status of the Docker Cloud automated builds (appropriate access rights are required):
* [transmart-server](https://cloud.docker.com/swarm/thehyve/repository/docker/thehyve/transmart-core/builds)
* [transmart-database](https://cloud.docker.com/swarm/thehyve/repository/docker/thehyve/transmart-database/builds)
