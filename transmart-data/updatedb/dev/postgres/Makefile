migrate:
	$(PGSQL_BIN)/psql -f ../../../ddl/postgres/biomart_user/query.sql
	$(PGSQL_BIN)/psql -f ../../../ddl/postgres/i2b2metadata/i2b2_tag_types.sql
	$(PGSQL_BIN)/psql -f ../../../ddl/postgres/i2b2metadata/i2b2_tag_options.sql
	$(PGSQL_BIN)/psql -f ../../../ddl/postgres/i2b2demodata/relation_type.sql
	$(PGSQL_BIN)/psql -f ../../../ddl/postgres/i2b2demodata/relation.sql
	$(PGSQL_BIN)/psql -f data_migration.sql
	$(PGSQL_BIN)/psql -f ../common/data_migration.sql
