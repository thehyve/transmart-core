migrate:
	$(MAKE) -C ../../../ddl/postgres/GLOBAL pg_bitcount_extension
	$(PGSQL_BIN)/psql -f migrate.sql
	$(MAKE) -C ../../../data/postgres refresh_mvs
