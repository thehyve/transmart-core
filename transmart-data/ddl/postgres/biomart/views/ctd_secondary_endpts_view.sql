--
-- Name: ctd_secondary_endpts_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_secondary_endpts_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.secondary_type) AS id, v.ref_article_protocol_id, v.secondary_type, v.secondary_type_definition, v.secondary_type_change, v.secondary_type_time_period, v.secondary_type_p_value, v.secondary_type_stat_test FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.secondary_type, ctd_full.secondary_type_definition, ctd_full.secondary_type_change, ctd_full.secondary_type_time_period, ctd_full.secondary_type_p_value, ctd_full.secondary_type_stat_test FROM ctd_full WHERE (((ctd_full.secondary_type IS NOT NULL) AND ((ctd_full.secondary_type)::text <> ''::text)) OR ((ctd_full.secondary_type_definition IS NOT NULL) AND ((ctd_full.secondary_type_definition)::text <> ''::text))) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.secondary_type) v;

