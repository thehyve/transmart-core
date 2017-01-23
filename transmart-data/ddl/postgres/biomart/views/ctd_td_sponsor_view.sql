--
-- Name: ctd_td_sponsor_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_sponsor_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.sponsor, v.trial_nbr_of_patients_studied) AS id, v.ref_article_protocol_id, v.sponsor, v.trial_nbr_of_patients_studied, v.source_type FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.sponsor, ctd_full.trial_nbr_of_patients_studied, ctd_full.source_type FROM ctd_full WHERE (((ctd_full.sponsor IS NOT NULL) AND ((ctd_full.sponsor)::text <> ''::text)) OR ((ctd_full.trial_nbr_of_patients_studied IS NOT NULL) AND ((ctd_full.trial_nbr_of_patients_studied)::text <> ''::text))) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.sponsor, ctd_full.trial_nbr_of_patients_studied) v;

