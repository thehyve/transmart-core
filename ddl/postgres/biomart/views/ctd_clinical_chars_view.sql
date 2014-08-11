--
-- Name: ctd_clinical_chars_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_clinical_chars_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.clinical_variable) AS id, v.ref_article_protocol_id, v.clinical_variable, v.clinical_variable_pct, v.clinical_variable_value FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.clinical_variable, ctd_full.clinical_variable_pct, ctd_full.clinical_variable_value FROM ctd_full WHERE ((ctd_full.clinical_variable IS NOT NULL) AND ((ctd_full.clinical_variable)::text <> ''::text)) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.clinical_variable) v;

