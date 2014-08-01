--
-- Name: bio_marker_correl_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_marker_correl_view AS
    (SELECT DISTINCT b.bio_marker_id, b.bio_marker_id AS asso_bio_marker_id, 'GENE'::text AS correl_type, 1 AS mv_id FROM bio_marker b WHERE ((b.bio_marker_type)::text = 'GENE'::text) UNION SELECT DISTINCT c.bio_data_id AS bio_marker_id, c.asso_bio_data_id AS asso_bio_marker_id, 'PATHWAY_GENE'::text AS correl_type, 2 AS mv_id FROM bio_marker b, bio_data_correlation c, bio_data_correl_descr d WHERE ((((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((b.primary_source_code)::text <> 'ARIADNE'::text)) AND ((d.correlation)::text = 'PATHWAY GENE'::text))) UNION SELECT DISTINCT c.bio_data_id AS bio_marker_id, c.asso_bio_data_id AS asso_bio_marker_id, 'HOMOLOGENE_GENE'::text AS correl_type, 3 AS mv_id FROM bio_marker b, bio_data_correlation c, bio_data_correl_descr d WHERE (((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id)) AND ((d.correlation)::text = 'HOMOLOGENE GENE'::text));

