# Test studies

This folder contains test studies ready to be loaded by `transmart-copy`.

## How to load test studies

### transmart-schema

When the database was created with the Liquibase scripts from
[transmart-schemas](../../transmart-schemas), data is loaded using
the `biomart_user` user.
Assuming a copy of `transmart-copy.jar` (see [transmart-copy](../../transmart-copy))
in the current directory, the test studies can be loaded as follows:

```bash
export PGUSER=biomart_user PGPASSWORD=biomart_user
for d in $(ls -d */); do
  java -jar transmart-copy.jar -d "$d"
done
java -jar transmart-copy.jar -m pedigree -d SURVEYS
```

### transmart-data

When the database was created using transmart-data,
the data loading tool uses the `tm_cz` user.
Before you load test data, make sure you source the `vars` file with the `TM_CZ_PWD` environment variable specified.
This assumes that the database schemas have been created using `transmart-data`.
If the schemas have been created following the instructions in [transmart-schemas](../../transmart-schemas),
the studies can still be loaded, but with different credentials.  

To load test studies run the following make command:
```
	make load
```

The command will make sure you have specified (see `TRANSMART_COPY_VERSION` in the Makefile) transmart copy version in the current directory. If not, it downloads it from the nexus repository.
