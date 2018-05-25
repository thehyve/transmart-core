migrate:
	$(MAKE) -C ../../../ddl/postgres/GLOBAL pg_bitcount_extension
	$(PGSQL_BIN)/psql -f migrate.sql
	$(MAKE) -C ../../../data/postgres refresh_mvs

fix_test_data:
	bash reload_patient_mappings.sh
	$(PGSQL_BIN)/psql -f fix_neg_patient_nums.sql