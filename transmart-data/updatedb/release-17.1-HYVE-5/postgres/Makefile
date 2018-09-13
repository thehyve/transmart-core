migrate:
	$(PGSQL_BIN)/psql --single-transaction -f ../common/migrate.sql
	$(PGSQL_BIN)/psql --single-transaction -f migrate_bitset.sql
	$(PGSQL_BIN)/psql -f to_date_data_type.sql
	$(PGSQL_BIN)/psql -f increase_nval_num_scale.sql
	$(PGSQL_BIN)/psql -f observation_fact_idx_pkey_and_trial_visit.sql
