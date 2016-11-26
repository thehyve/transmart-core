# Dockerized test database for transmart
install docker 1.12+ and do
```bash
     docker-compose up
```
inside this directory to start a postgres database with test data loaded
it will listen on port 6000

in order to investigate the database via psql:
```bash
    docker exec -it docker_tm_test_db_1 bash
    sudo su postgres
    psql transmart
```
