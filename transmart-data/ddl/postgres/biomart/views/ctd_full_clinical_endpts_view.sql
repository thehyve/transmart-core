--
-- Name: ctd_full_clinical_endpts_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_full_clinical_endpts_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id,
    v.ref_article_protocol_id,
    v.primary_endpoint_type,
    v.primary_endpoint_definition,
    v.primary_endpoint_change,
    v.primary_endpoint_time_period,
    v.primary_endpoint_p_value,
    v.primary_endpoint_stat_test,
    v.secondary_type,
    v.secondary_type_definition,
    v.secondary_type_change,
    v.secondary_type_time_period,
    v.secondary_type_p_value,
    v.secondary_type_stat_test
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.primary_endpoint_type,
            ctd_full.primary_endpoint_definition,
            ctd_full.primary_endpoint_change,
            ctd_full.primary_endpoint_time_period,
            ctd_full.primary_endpoint_p_value,
            ctd_full.primary_endpoint_stat_test,
            ctd_full.secondary_type,
            ctd_full.secondary_type_definition,
            ctd_full.secondary_type_change,
            ctd_full.secondary_type_time_period,
            ctd_full.secondary_type_p_value,
            ctd_full.secondary_type_stat_test
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id) v;

