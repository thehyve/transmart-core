--
-- Name: ctd_study_details_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_study_details_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.common_name) AS id, v.ref_article_protocol_id, v.study_type, v.common_name, v.icd10, v.mesh, v.disease_type, v.physiology_name FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.study_type, ctd_full.common_name, ctd_full.icd10, ctd_full.mesh, ctd_full.disease_type, ctd_full.physiology_name FROM ctd_full WHERE ((ctd_full.common_name IS NOT NULL) AND ((ctd_full.common_name)::text <> ''::text)) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.common_name) v;

