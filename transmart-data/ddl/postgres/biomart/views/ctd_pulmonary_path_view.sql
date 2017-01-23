--
-- Name: ctd_pulmonary_path_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_pulmonary_path_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.pulmonary_pathology_name) AS id, v.ref_article_protocol_id, v.pulmonary_pathology_name, v.pulmpath_patient_pct, v.pulmpath_value_unit, v.pulmpath_method FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.pulmonary_pathology_name, ctd_full.pulmpath_patient_pct, ctd_full.pulmpath_value_unit, ctd_full.pulmpath_method FROM ctd_full WHERE ((ctd_full.pulmonary_pathology_name IS NOT NULL) AND ((ctd_full.pulmonary_pathology_name)::text <> ''::text)) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.pulmonary_pathology_name) v;

