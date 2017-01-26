FILES = \
	import_xnat_configuration \
	import_xnat_variable \
	xnat_subject \
	functions/i2b2_load_clinical_data \
	functions/i2b2_load_rbm_data \
	functions/i2b2_load_study_metadata \
	functions/i2b2_process_acgh_data \
	functions/i2b2_process_metabolomic_data \
	functions/i2b2_process_mrna_data \
	functions/i2b2_process_proteomics_data \
	functions/i2b2_process_qpcr_mirna_data \
	functions/i2b2_process_rna_seq_data \
	functions/i2b2_proteomics_zscore_calc \
	functions/i2b2_rename_node \
	functions/i2b2_rna_seq_zscore_calc \
	functions/i2b2_secure_study \
	functions/rdc_reload_mrna_data \
	functions/upgrade_mrna_data \
    alter

migrate:
	$(PGSQL_BIN)/psql $(foreach u,$(FILES),-f $(u).sql)
