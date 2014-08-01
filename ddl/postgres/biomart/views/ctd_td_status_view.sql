--
-- Name: ctd_td_status_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_status_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id, v.ref_article_protocol_id, v.trial_status, v.trial_phase FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.trial_status, ctd_full.trial_phase FROM ctd_full WHERE (((ctd_full.trial_status IS NOT NULL) AND ((ctd_full.trial_status)::text <> ''::text)) OR ((ctd_full.trial_phase IS NOT NULL) AND ((ctd_full.trial_phase)::text <> ''::text))) ORDER BY ctd_full.ref_article_protocol_id) v;

