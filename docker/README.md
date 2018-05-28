Docker image definitions and docker-compose profiles to run tranSMART and its dependencies.
`transmart-server` runs the tranSMART application server through WildFly, and `transmart-database` its database through PostgreSQL.

# Usage
Run in this directory:
```
docker-compose -f docker-compose.xxx.yml up
```
Which will build/download the images, and run the containers.

*Important note*: if this is the very first time you run this command, the `transmart-database` container will take some time to load its test data.
As a result `transmart-server` will fail its startup.
Simply wait for `transmart-database` to be initialised and restart the containers.

# Docker-compose profiles
* `docker-compose.src.yml`: builds the different images from source, and starts `transmart-server`, `transmart-database` and `solr`
* `docker-compose.bin.yml`: downloads the different images from Docker Hub, and starts `transmart-server` and `transmart-database`

With those profiles, the ports exposed by the containers are shifted by +5, e.g. the transmart API endpoint will be `http://localhost:8085/transmart-server`.
