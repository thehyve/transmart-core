--
-- Name: search_taxonomy_level1; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level1 AS
    SELECT st.term_id, st.term_name, sc.category_name FROM search_taxonomy_rels str, search_taxonomy st, search_categories sc WHERE ((str.parent_id = sc.category_id) AND (str.child_id = st.term_id));

