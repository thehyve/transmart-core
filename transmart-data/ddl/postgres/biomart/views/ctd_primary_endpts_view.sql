--
-- Name: ctd_primary_endpts_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_primary_endpts_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.primary_endpoint_type) AS id,
    v.ref_article_protocol_id,
    v.primary_endpoint_type,
    v.primary_endpoint_definition,
    v.primary_endpoint_change,
    v.primary_endpoint_time_period,
    v.primary_endpoint_p_value,
    v.primary_endpoint_stat_test
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.primary_endpoint_type,
            ctd_full.primary_endpoint_definition,
            ctd_full.primary_endpoint_change,
            ctd_full.primary_endpoint_time_period,
            ctd_full.primary_endpoint_p_value,
            ctd_full.primary_endpoint_stat_test
           FROM ctd_full
          WHERE (((ctd_full.primary_endpoint_type IS NOT NULL) AND ((ctd_full.primary_endpoint_type)::text <> ''::text)) OR ((ctd_full.primary_endpoint_definition IS NOT NULL) AND ((ctd_full.primary_endpoint_definition)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.primary_endpoint_type) v;

