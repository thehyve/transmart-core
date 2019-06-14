migrate:
	$(PGSQL_BIN)/psql --single-transaction -f dimension_description_columns.sql
	$(PGSQL_BIN)/psql --single-transaction -f ../common/dimension_description_updates.sql
	$(PGSQL_BIN)/psql --single-transaction -f patient_set_collection_index.sql
