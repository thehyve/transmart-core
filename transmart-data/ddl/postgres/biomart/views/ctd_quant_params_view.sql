--
-- Name: ctd_quant_params_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_quant_params_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id,
    v.ref_article_protocol_id,
    v.clinical_variable_name,
    v.pct_change_from_baseline,
    v.abs_change_from_baseline,
    v.rate_of_change_from_baseline,
    v.average_over_treatment_period,
    v.within_group_changes,
    v.stat_measure_p_value
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.clinical_variable_name,
            ctd_full.pct_change_from_baseline,
            ctd_full.abs_change_from_baseline,
            ctd_full.rate_of_change_from_baseline,
            ctd_full.average_over_treatment_period,
            ctd_full.within_group_changes,
            ctd_full.stat_measure_p_value
           FROM ctd_full
          WHERE ((ctd_full.clinical_variable_name IS NOT NULL) AND ((ctd_full.clinical_variable_name)::text <> ''::text))
          ORDER BY ctd_full.ref_article_protocol_id) v;

