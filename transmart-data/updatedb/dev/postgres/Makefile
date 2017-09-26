migrate:
	$(PGSQL_BIN)/psql -f ../common/data_migration.sql
	$(PGSQL_BIN)/psql -f data_migration.sql
	$(PGSQL_BIN)/psql -f ../../../ddl/postgres/biomart_user/query.sql
