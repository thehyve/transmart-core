FILES = \
	study \
	trial_visit_dimension \
	observation_fact \
	dimension_description \
	study_dimension_descriptions \
	de_rnaseq_transcript_annot \
	de_rnaseq_transcript_data \
	supported_workflow \
	storage_system \
	linked_file_collection \
	data_migration

upgrade:
	$(PGSQL_BIN)/psql $(foreach u,$(FILES),-f $(u).sql)
