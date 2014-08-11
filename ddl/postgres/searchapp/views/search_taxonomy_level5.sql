--
-- Name: search_taxonomy_level5; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level5 AS
    SELECT st.term_id, st.term_name, stl4.category_name FROM search_taxonomy_rels str, search_taxonomy st, search_taxonomy_level4 stl4 WHERE ((str.parent_id = stl4.term_id) AND (str.child_id = st.term_id));

