include ../lib/makefile.inc

postgres_test_data: $(JDBC_DRIVER_PSQL_PATH)
	find . -name *.tsv | groovy parse_and_reorder.groovy | groovy load_to_postgres.groovy

postgres_clean_all_data: $(JDBC_DRIVER_PSQL_PATH)
	find . -name *.tsv | groovy parse_and_reorder.groovy | groovy clean_postgres_tables.groovy

oracle_test_data: $(JDBC_DRIVER_ORA_PATH) $(JDBC_XDB_ORA_PATH)
	find . -name *.tsv | groovy parse_and_reorder.groovy | NLS_DATE_FORMAT='yyyy-mm-dd hh24:mi:ss' \
    NLS_TIMESTAMP_FORMAT='yyyy-mm-dd hh24:mi:ss.ff' \
    groovy -cp '$(CP_ORA)' load_to_oracle.groovy
