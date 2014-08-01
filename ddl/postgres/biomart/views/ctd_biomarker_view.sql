--
-- Name: ctd_biomarker_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_biomarker_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomarker_name) AS id, v.ref_article_protocol_id, v.biomarker_name, v.biomarker_pct, v.biomarker_value FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.biomarker_name, ctd_full.biomarker_pct, ctd_full.biomarker_value FROM ctd_full WHERE ((ctd_full.biomarker_name IS NOT NULL) AND ((ctd_full.biomarker_name)::text <> ''::text)) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomarker_name) v;

