migrate:
	$(PGSQL_BIN)/psql --single-transaction -f ../common/migrate.sql
	$(PGSQL_BIN)/psql --single-transaction -f migrate_bitset.sql
