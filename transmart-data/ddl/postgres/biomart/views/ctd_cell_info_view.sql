--
-- Name: ctd_cell_info_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_cell_info_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.cellinfo_type) AS id, v.ref_article_protocol_id, v.cellinfo_type, v.cellinfo_count, v.cellinfo_source FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.cellinfo_type, ctd_full.cellinfo_count, ctd_full.cellinfo_source FROM ctd_full WHERE ((ctd_full.cellinfo_type IS NOT NULL) AND ((ctd_full.cellinfo_type)::text <> ''::text)) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.cellinfo_type) v;

