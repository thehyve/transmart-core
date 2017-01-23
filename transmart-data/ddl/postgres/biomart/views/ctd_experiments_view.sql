--
-- Name: ctd_experiments_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_experiments_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.drug_inhibitor_common_name, v.drug_inhibitor_trtmt_regime) AS id, v.ref_article_protocol_id, v.drug_inhibitor_common_name, v.drug_inhibitor_dose, v.drug_inhibitor_time_period, v.drug_inhibitor_route_of_admin, v.drug_inhibitor_trtmt_regime, v.comparator_name, v.comparator_dose, v.comparator_time_period, v.comparator_route_of_admin, v.treatment_regime, v.placebo, v.experiment_description FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.drug_inhibitor_common_name, ctd_full.drug_inhibitor_time_period, ctd_full.drug_inhibitor_dose, ctd_full.drug_inhibitor_route_of_admin, ctd_full.drug_inhibitor_trtmt_regime, ctd_full.comparator_name, ctd_full.comparator_dose, ctd_full.comparator_time_period, ctd_full.comparator_route_of_admin, ctd_full.treatment_regime, ctd_full.placebo, ctd_full.experiment_description FROM ctd_full ORDER BY ctd_full.ref_article_protocol_id, ctd_full.drug_inhibitor_common_name, ctd_full.drug_inhibitor_trtmt_regime) v;

