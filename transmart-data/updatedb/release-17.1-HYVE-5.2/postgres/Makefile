migrate:
	$(MAKE) -C ../../../ddl/postgres/GLOBAL pg_bitcount_extension
	$(PGSQL_BIN)/psql --single-transaction -f migrate_bitset.sql
