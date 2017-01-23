--
-- Name: ctd_stats_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_stats_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.statistical_test) AS id, v.ref_article_protocol_id, v.clinical_correlation, v.statistical_test, v.statistical_coefficient_value, v.statistical_test_p_value, v.statistical_test_description FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.clinical_correlation, ctd_full.statistical_test, ctd_full.statistical_coefficient_value, ctd_full.statistical_test_p_value, ctd_full.statistical_test_description FROM ctd_full WHERE (((ctd_full.statistical_test_description IS NOT NULL) AND ((ctd_full.statistical_test_description)::text <> ''::text)) OR ((ctd_full.statistical_test IS NOT NULL) AND ((ctd_full.statistical_test)::text <> ''::text))) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.statistical_test) v;

