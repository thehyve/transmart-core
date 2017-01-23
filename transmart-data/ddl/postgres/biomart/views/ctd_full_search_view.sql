--
-- Name: ctd_full_search_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_full_search_view AS
    SELECT row_number() OVER (ORDER BY t.ref_article_protocol_id) AS fact_id, t.ref_article_protocol_id, t.mesh, t.common_name, t.drug_inhibitor_standard_name, t.primary_endpoint_type, t.secondary_type, t.biomarker_name, t.disease_severity, t.inhaled_steroid_dose, t.fev1, t.primary_endpoint_time_period, t.primary_endpoint_change, t.primary_endpoint_p_value FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.mesh, ctd_full.common_name, ctd_full.drug_inhibitor_standard_name, ctd_full.primary_endpoint_type, ctd_full.secondary_type, ctd_full.biomarker_name, ctd_full.disease_severity, ctd_full.inhaled_steroid_dose, ctd_full.fev1, ctd_full.primary_endpoint_time_period, ctd_full.primary_endpoint_change, ctd_full.primary_endpoint_p_value FROM ctd_full) t;

