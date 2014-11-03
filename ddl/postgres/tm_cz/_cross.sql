ALTER FUNCTION cz_end_audit(numeric, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION cz_error_handler(numeric, character varying, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION cz_start_audit(character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION cz_write_audit(numeric, character varying, character varying, character varying, numeric, numeric, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION cz_write_error(numeric, character varying, character varying, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION cz_write_info(numeric, numeric, numeric, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_array_sort(anyarray) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION czx_end_audit(numeric, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_error_handler(numeric, character varying, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_info_handler(numeric, numeric, numeric, character varying, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_percentile_cont(real[], real) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION czx_start_audit(character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_table_index_maint(character varying, character varying, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, deapp, i2b2demodata, pg_temp;

ALTER FUNCTION czx_write_audit(numeric, character varying, character varying, character varying, numeric, numeric, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_write_error(numeric, character varying, character varying, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION czx_write_info(numeric, numeric, numeric, character varying, character varying) SET search_path TO tm_cz, pg_temp;

ALTER FUNCTION i2b2_add_node(character varying, character varying, character varying, numeric) SET search_path TO tm_cz, i2b2metadata, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_add_root_node(character varying, numeric) SET search_path TO tm_cz, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_backout_trial(character varying, character varying, numeric) SET search_path TO tm_cz, i2b2metadata, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_create_concept_counts(character varying, numeric) SET search_path TO tm_cz, i2b2demodata, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_create_security_for_trial(character varying, character varying, numeric) SET search_path TO tm_cz, i2b2demodata, i2b2metadata, searchapp, biomart, pg_temp;

ALTER FUNCTION i2b2_delete_1_node(character varying) SET search_path TO tm_cz, i2b2demodata, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_delete_all_nodes(character varying, numeric) SET search_path TO tm_cz, i2b2demodata, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_extend_clinical_data(character varying, character varying, character varying, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION i2b2_fill_in_tree(character varying, character varying, numeric) SET search_path TO tm_cz, i2b2demodata, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_hide_node(character varying) SET search_path TO tm_cz, i2b2metadata, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_load_annotation_deapp(numeric) SET search_path TO tm_cz, deapp, biomart, pg_temp;

ALTER FUNCTION i2b2_load_chrom_region(character varying, character varying, character varying, numeric) SET search_path TO tm_cz, deapp;

ALTER FUNCTION i2b2_load_clinical_data(character varying, character varying, character varying, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION i2b2_load_gwas_top50(numeric, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, biomart, biomart_stage, pg_temp;

ALTER FUNCTION i2b2_load_metabolomics_annot(numeric) SET search_path TO tm_cz, i2b2metadata, pg_temp, deapp, tm_lz, tm_wz, biomart;

ALTER FUNCTION i2b2_load_mirna_annot_deapp(numeric) SET search_path TO tm_cz, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_load_proteomics_annot(numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION i2b2_load_security_data(numeric) SET search_path TO tm_cz, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_load_security_data(character varying, numeric) SET search_path TO tm_cz, i2b2metadata, pg_temp;

ALTER FUNCTION i2b2_move_analysis_to_prod(numeric, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, biomart, biomart_stage, pg_temp;

ALTER FUNCTION i2b2_move_study(character varying, character varying, numeric) SET search_path TO tm_cz, i2b2metadata, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_process_acgh_data(character varying, character varying, character varying, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, deapp, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_process_metabolomic_data(character varying, character varying, character varying, character varying, bigint, character varying, bigint) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION i2b2_process_mrna_data(character varying, character varying, character varying, character varying, numeric, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, deapp, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_process_proteomics_data(character varying, character varying, character varying, character varying, numeric, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION i2b2_process_qpcr_mirna_data(character varying, character varying, character varying, character varying, numeric, character varying, numeric, character varying) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

ALTER FUNCTION i2b2_process_rnaseq_data(character varying, character varying, character varying, character varying, character varying, numeric, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, deapp, i2b2demodata, pg_temp;

ALTER FUNCTION i2b2_proteomics_zscore_calc(character varying, character varying, numeric, character varying, numeric, character varying) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;

--
-- Name: czv_pivot_sample_categories; Type: VIEW; Schema: tm_cz; Owner: -
--
CREATE VIEW czv_pivot_sample_categories AS
    SELECT x.trial_cd, x.sample_cd AS sample_id, x.trial_name, COALESCE(x.pathology, 'Not Applicable'::text) AS pathology, COALESCE(x.race, 'Not Applicable'::text) AS race, COALESCE(x.tissue_type, 'Not Applicable'::text) AS tissue_type, COALESCE(x.gender, 'Not Applicable'::text) AS gender, COALESCE(x.biomarker, 'Not Applicable'::text) AS biomarker, COALESCE(x.access_type, 'Not Applicable'::text) AS access_type, COALESCE(x.institution, 'Not Applicable'::text) AS institution, COALESCE(x.program_initiative, 'Not Applicable'::text) AS program_initiative, COALESCE(x.organism, 'Not Applicable'::text) AS organism FROM (SELECT s.trial_cd, s.sample_cd, f.c_name AS trial_name, max((CASE WHEN ((s.category_cd)::text = 'PATHOLOGY'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS pathology, max((CASE WHEN ((s.category_cd)::text = 'RACE'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS race, max((CASE WHEN ((s.category_cd)::text = 'TISSUE_TYPE'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS tissue_type, max((CASE WHEN ((s.category_cd)::text = 'GENDER'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS gender, max((CASE WHEN ((s.category_cd)::text = 'BIOMARKER'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS biomarker, max((CASE WHEN ((s.category_cd)::text = 'ACCESS'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS access_type, max((CASE WHEN ((s.category_cd)::text = 'INSTITUTION'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS institution, max((CASE WHEN ((s.category_cd)::text = 'PROGRAM/INITIATIVE'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS program_initiative, max((CASE WHEN ((s.category_cd)::text = 'ORGANISM'::text) THEN s.category_value ELSE NULL::character varying END)::text) AS organism FROM tm_lz.lz_src_sample_categories s, i2b2metadata.i2b2 f WHERE (((s.trial_cd)::text = (f.sourcesystem_cd)::text) AND (f.c_hlevel = (SELECT min(x_1.c_hlevel) AS min FROM i2b2metadata.i2b2 x_1 WHERE ((f.sourcesystem_cd)::text = (x_1.sourcesystem_cd)::text)))) GROUP BY s.trial_cd, s.sample_cd, f.c_name) x;

