--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: searchapp; Owner: -
--
CREATE SEQUENCE hibernate_sequence
    START WITH 100041
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


SET default_with_oids = false;

--
-- Name: listsig_genes; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW listsig_genes AS
 SELECT k_gsi.search_keyword_id AS gene_keyword_id, 
    k_gs.search_keyword_id AS list_keyword_id
   FROM search_keyword k_gs, 
    search_gene_signature gs, 
    search_gene_signature_item gsi, 
    search_keyword k_gsi
  WHERE (((k_gs.bio_data_id = gs.search_gene_signature_id) AND (gs.search_gene_signature_id = gsi.search_gene_signature_id)) AND (gsi.bio_marker_id = k_gsi.bio_data_id));

--
-- Name: pathway_genes; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW pathway_genes AS
 SELECT k_gene.search_keyword_id AS gene_keyword_id, 
    k_pathway.search_keyword_id AS pathway_keyword_id, 
    b.asso_bio_marker_id AS gene_biomarker_id
   FROM search_keyword k_pathway, 
    biomart.bio_marker_correl_mv b, 
    search_keyword k_gene
  WHERE (((((b.correl_type = 'PATHWAY_GENE'::text) AND (b.bio_marker_id = k_pathway.bio_data_id)) AND ((k_pathway.data_category)::text = 'PATHWAY'::text)) AND (b.asso_bio_marker_id = k_gene.bio_data_id)) AND ((k_gene.data_category)::text = 'GENE'::text));

--
-- Name: plugin_module_seq; Type: SEQUENCE; Schema: searchapp; Owner: -
--
CREATE SEQUENCE plugin_module_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

--
-- Name: plugin_seq; Type: SEQUENCE; Schema: searchapp; Owner: -
--
CREATE SEQUENCE plugin_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

--
-- Name: search_auth_user_sec_access_v; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_auth_user_sec_access_v AS
        (         SELECT sasoa.auth_sec_obj_access_id AS search_auth_user_sec_access_id, 
                    sasoa.auth_principal_id AS search_auth_user_id, 
                    sasoa.secure_object_id AS search_secure_object_id, 
                    sasoa.secure_access_level_id AS search_sec_access_level_id
                   FROM search_auth_user sau, 
                    search_auth_sec_object_access sasoa
                  WHERE (sau.id = sasoa.auth_principal_id)
        UNION 
                 SELECT sasoa.auth_sec_obj_access_id AS search_auth_user_sec_access_id, 
                    sagm.auth_user_id AS search_auth_user_id, 
                    sasoa.secure_object_id AS search_secure_object_id, 
                    sasoa.secure_access_level_id AS search_sec_access_level_id
                   FROM search_auth_group sag, 
                    search_auth_group_member sagm, 
                    search_auth_sec_object_access sasoa
                  WHERE ((sag.id = sagm.auth_group_id) AND (sag.id = sasoa.auth_principal_id)))
UNION 
         SELECT sasoa.auth_sec_obj_access_id AS search_auth_user_sec_access_id, 
            NULL::bigint AS search_auth_user_id, 
            sasoa.secure_object_id AS search_secure_object_id, 
            sasoa.secure_access_level_id AS search_sec_access_level_id
           FROM search_auth_group sag, 
            search_auth_sec_object_access sasoa
          WHERE (((sag.group_category)::text = 'EVERYONE_GROUP'::text) AND (sag.id = sasoa.auth_principal_id));

--
-- Name: search_bio_mkr_correl_fast_view; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_bio_mkr_correl_fast_view AS
 SELECT i.search_gene_signature_id AS domain_object_id, 
    i.bio_marker_id AS asso_bio_marker_id, 
    'GENE_SIGNATURE_ITEM' AS correl_type, 
        CASE
            WHEN (i.fold_chg_metric IS NULL) THEN (1)::bigint
            ELSE i.fold_chg_metric
        END AS value_metric, 
    3 AS mv_id
   FROM search_gene_signature_item i, 
    search_gene_signature gs
  WHERE ((i.search_gene_signature_id = gs.search_gene_signature_id) AND (gs.deleted_flag = false));

--
-- Name: search_categories; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_categories AS
 SELECT str.child_id AS category_id, 
    st.term_name AS category_name
   FROM search_taxonomy_rels str, 
    search_taxonomy st
  WHERE ((str.parent_id = ( SELECT search_taxonomy_rels.child_id
           FROM search_taxonomy_rels
          WHERE (search_taxonomy_rels.parent_id IS NULL))) AND (str.child_id = st.term_id));

--
-- Name: search_taxonomy_level1; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level1 AS
 SELECT st.term_id, 
    st.term_name, 
    sc.category_name
   FROM search_taxonomy_rels str, 
    search_taxonomy st, 
    search_categories sc
  WHERE ((str.parent_id = sc.category_id) AND (str.child_id = st.term_id));

--
-- Name: search_taxonomy_level2; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level2 AS
 SELECT st.term_id, 
    st.term_name, 
    stl1.category_name
   FROM search_taxonomy_rels str, 
    search_taxonomy st, 
    search_taxonomy_level1 stl1
  WHERE ((str.parent_id = stl1.term_id) AND (str.child_id = st.term_id));

--
-- Name: search_taxonomy_level3; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level3 AS
 SELECT st.term_id, 
    st.term_name, 
    stl2.category_name
   FROM search_taxonomy_rels str, 
    search_taxonomy st, 
    search_taxonomy_level2 stl2
  WHERE ((str.parent_id = stl2.term_id) AND (str.child_id = st.term_id));

--
-- Name: search_taxonomy_level4; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level4 AS
 SELECT st.term_id, 
    st.term_name, 
    stl3.category_name
   FROM search_taxonomy_rels str, 
    search_taxonomy st, 
    search_taxonomy_level3 stl3
  WHERE ((str.parent_id = stl3.term_id) AND (str.child_id = st.term_id));

--
-- Name: search_taxonomy_level5; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level5 AS
 SELECT st.term_id, 
    st.term_name, 
    stl4.category_name
   FROM search_taxonomy_rels str, 
    search_taxonomy st, 
    search_taxonomy_level4 stl4
  WHERE ((str.parent_id = stl4.term_id) AND (str.child_id = st.term_id));

--
-- Name: search_taxonomy_lineage; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_lineage AS
 SELECT s1.child_id, 
    s2.child_id AS parent1, 
    s3.child_id AS parent2, 
    s4.child_id AS parent3, 
    s5.child_id AS parent4
   FROM search_taxonomy_rels s1, 
    search_taxonomy_rels s2, 
    search_taxonomy_rels s3, 
    search_taxonomy_rels s4, 
    search_taxonomy_rels s5
  WHERE ((((s1.parent_id = s2.child_id) AND (s2.parent_id = s3.child_id)) AND (s3.parent_id = s4.child_id)) AND (s4.parent_id = s5.child_id));

--
-- Name: search_taxonomy_terms_cats; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_terms_cats AS
 SELECT DISTINCT unoin_results.term_id, 
    unoin_results.term_name, 
    unoin_results.category_name
   FROM (        (        (        (         SELECT search_taxonomy_level1.term_id, 
                                            search_taxonomy_level1.term_name, 
                                            search_taxonomy_level1.category_name
                                           FROM search_taxonomy_level1
                                UNION 
                                         SELECT search_taxonomy_level2.term_id, 
                                            search_taxonomy_level2.term_name, 
                                            search_taxonomy_level2.category_name
                                           FROM search_taxonomy_level2)
                        UNION 
                                 SELECT search_taxonomy_level3.term_id, 
                                    search_taxonomy_level3.term_name, 
                                    search_taxonomy_level3.category_name
                                   FROM search_taxonomy_level3)
                UNION 
                         SELECT search_taxonomy_level4.term_id, 
                            search_taxonomy_level4.term_name, 
                            search_taxonomy_level4.category_name
                           FROM search_taxonomy_level4)
        UNION 
                 SELECT search_taxonomy_level5.term_id, 
                    search_taxonomy_level5.term_name, 
                    search_taxonomy_level5.category_name
                   FROM search_taxonomy_level5) unoin_results;

--
-- Name: solr_keywords_lineage; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW solr_keywords_lineage AS
 SELECT DISTINCT union_results.term_id, 
    union_results.ancestor_id, 
    union_results.search_keyword_id
   FROM (        (        (        (         SELECT DISTINCT l.child_id AS term_id, 
                                            l.child_id AS ancestor_id, 
                                            st.search_keyword_id
                                           FROM search_taxonomy_lineage l, 
                                            search_taxonomy st
                                          WHERE ((l.child_id = st.term_id) AND (l.child_id IS NOT NULL))
                                UNION 
                                         SELECT DISTINCT l.child_id AS term_id, 
                                            l.parent1 AS ancestor_id, 
                                            st.search_keyword_id
                                           FROM search_taxonomy_lineage l, 
                                            search_taxonomy st
                                          WHERE ((l.parent1 = st.term_id) AND (l.parent1 IS NOT NULL)))
                        UNION 
                                 SELECT DISTINCT l.child_id AS term_id, 
                                    l.parent2 AS ancestor_id, 
                                    st.search_keyword_id
                                   FROM search_taxonomy_lineage l, 
                                    search_taxonomy st
                                  WHERE ((l.parent2 = st.term_id) AND (l.parent2 IS NOT NULL)))
                UNION 
                         SELECT DISTINCT l.child_id AS term_id, 
                            l.parent3 AS ancestor_id, 
                            st.search_keyword_id
                           FROM search_taxonomy_lineage l, 
                            search_taxonomy st
                          WHERE ((l.parent3 = st.term_id) AND (l.parent3 IS NOT NULL)))
        UNION 
                 SELECT DISTINCT l.child_id AS term_id, 
                    l.parent4 AS ancestor_id, 
                    st.search_keyword_id
                   FROM search_taxonomy_lineage l, 
                    search_taxonomy st
                  WHERE ((l.parent4 = st.term_id) AND (l.parent4 IS NOT NULL))) union_results
  WHERE (union_results.search_keyword_id IS NOT NULL);

--
-- Name: gene_sig_parent_fk1; Type: FK CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_gene_signature
    ADD CONSTRAINT gene_sig_parent_fk1 FOREIGN KEY (parent_gene_signature_id) REFERENCES search_gene_signature(search_gene_signature_id);

