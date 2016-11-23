--
-- Name: ctd_td_design_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_design_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.nature_of_trial, v.trial_type) AS id, v.ref_article_protocol_id, v.nature_of_trial, v.randomization, v.blinded_trial, v.trial_type, v.run_in_period, v.treatment_period, v.washout_period, v.open_label_extension FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.nature_of_trial, ctd_full.randomization, ctd_full.blinded_trial, ctd_full.trial_type, ctd_full.run_in_period, ctd_full.treatment_period, ctd_full.washout_period, ctd_full.open_label_extension FROM ctd_full WHERE (((ctd_full.trial_type IS NOT NULL) AND ((ctd_full.trial_type)::text <> ''::text)) OR ((ctd_full.nature_of_trial IS NOT NULL) AND ((ctd_full.nature_of_trial)::text <> ''::text))) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.nature_of_trial, ctd_full.trial_type) v;

