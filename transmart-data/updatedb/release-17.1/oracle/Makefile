include ../../../lib/makefile.inc

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
	data_migration \
	qt_query_master \
	async_job \
	biomart_pf_to_tf \
	deapp_pf_to_tf \
	bio_marker_correl_mv \
	de_snp_subject_sorted_def \
	de_snp_subject_sorted_def_bk \
	i2b2_create_genotype_tree \
	i2b2_load_security_bystudy

RUN_SQL   = groovy -cp '$(CP_ORA)' ../../../ddl/oracle/_scripts/run_sql.groovy

migrate: $(JDBC_DRIVER_ORA_PATH)
	$(RUN_SQL) --sequential $(foreach u,$(FILES),-f $(u).sql)
