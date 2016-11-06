postgres_test_data:
	find . -name *.tsv | groovy parse_and_reorder.groovy | groovy load_to_postgres.groovy

postgres_clean_all_data:
	find . -name *.tsv | groovy parse_and_reorder.groovy | groovy clean_postgres_tables.groovy

oracle_test_data:
	find . -name *.tsv | groovy parse_and_reorder.groovy | NLS_DATE_FORMAT='yyyy-mm-dd hh24:mi:ss' \
    NLS_TIMESTAMP_FORMAT='yyyy-mm-dd hh24:mi:ss.ff' \
    groovy -cp '../lib/ojdbc6-12.1.0.2.jar:../lib/xdb.jar:../lib/' load_to_oracle.groovy
