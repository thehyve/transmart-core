--
-- Name: ctd_prior_med_use_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_prior_med_use_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.prior_med_drug_name) AS id, v.ref_article_protocol_id, v.prior_med_drug_name, v.prior_med_pct, v.prior_med_value FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.prior_med_drug_name, ctd_full.prior_med_pct, ctd_full.prior_med_value FROM ctd_full WHERE ((ctd_full.prior_med_drug_name IS NOT NULL) AND ((ctd_full.prior_med_drug_name)::text <> ''::text)) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.prior_med_drug_name) v;

