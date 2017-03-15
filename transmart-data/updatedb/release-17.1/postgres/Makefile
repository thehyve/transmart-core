migrate:
	$(PGSQL_BIN)/psql -f study.sql
	$(PGSQL_BIN)/psql -f trial_visit_dimension.sql
	$(PGSQL_BIN)/psql -f observation_fact.sql
	$(PGSQL_BIN)/psql -f dimension_description.sql
	$(PGSQL_BIN)/psql -f study_dimension_descriptions.sql
	$(PGSQL_BIN)/psql -f de_rnaseq_transcript_annot.sql
	$(PGSQL_BIN)/psql -f de_rnaseq_transcript_data.sql
	$(PGSQL_BIN)/psql -f supported_workflow.sql
	$(PGSQL_BIN)/psql -f storage_system.sql
	$(PGSQL_BIN)/psql -f linked_file_collection.sql
	$(PGSQL_BIN)/psql -f data_migration.sql
	$(PGSQL_BIN)/psql -f bio_marker_correl_mv.sql
