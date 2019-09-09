# Liquibase scripts


## Create database `transmartDev` with TranSMART schemas

- Create a user `sudo -u postgres psql -c "create role biomart_user with password 'biomart_user' login"`
- Set environment variables:
    ```bash
    PGUSER=biomart_user
    PGPASSWORD=biomart_user
    PGDATABASE=transmartDev
    ```
- Build the jar file with `gradle assemble` 
- Run [prepare_database.sh](../scripts/prepare_database.sh)

## Development

```bash
# Build the jar file in build/libs
gradle assemble

# Run test scripts
gradle check
```

To add schema updates, create a new changelog file in [db/changelog/changes](src/main/resources/db/changelog/changes)
and add a record in [db.changelog-master.yaml](src/main/resources/db/changelog/db.changelog-master.yaml).


### How to autogenerate a changelog

- Download [Liquibase](https://download.liquibase.org/)
- Download [postgresql-42.2.5.jre7.jar](https://search.maven.org/remotecontent?filepath=org/postgresql/postgresql/42.2.5.jre7/postgresql-42.2.5.jre7.jar)
- Create a file `liquibase.properties`:
    ```properties
    driver=org.postgresql.Driver
    classpath=postgresql-42.2.5.jre7.jar
    url=jdbc:postgresql://localhost:5432/transmart
    username=tm_cz
    password=tm_cz
    changeLogFile=db.changelog.xml
    ```

`/path/to/liquibase --defaultSchemaName=i2b2demodata generateChangelog`

This generates a changelog for the `i2b2demodata` schema in `db.changelog.xml`.
