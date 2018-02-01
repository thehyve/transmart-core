--
-- Name: vw_faceted_search_disease; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW vw_faceted_search_disease AS
 SELECT z.bio_assay_analysis_id,
    replace(ltrim(z.solr_hierarchy, '/'::text), '//'::text, '/'::text) AS solr_hierarchy
   FROM ( SELECT y.bio_assay_analysis_id,
            string_agg(y.path, '/'::text ORDER BY (ROW(y.*::record, y.path))::text) AS solr_hierarchy
           FROM ( SELECT x.bio_assay_analysis_id,
                    x.top_node,
                    max(x.path) AS path
                   FROM ( SELECT DISTINCT bdd.bio_data_id AS bio_assay_analysis_id,
                            substr(mp.path, 2, 11) AS top_node,
                            mp.path
                           FROM bio_data_disease bdd,
                            bio_disease bd,
                            ( SELECT ('DIS:'::text || (mesh.ui)::text) AS path,
                                    mesh.ui AS unique_id
                                   FROM mesh) mp
                          WHERE ((bdd.bio_disease_id = bd.bio_disease_id) AND ((bd.mesh_code)::text = (mp.unique_id)::text))) x
                  GROUP BY x.bio_assay_analysis_id, x.top_node
                  ORDER BY x.bio_assay_analysis_id) y
          GROUP BY y.bio_assay_analysis_id) z
  ORDER BY z.bio_assay_analysis_id;

