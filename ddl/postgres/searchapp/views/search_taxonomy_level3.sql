--
-- Name: search_taxonomy_level3; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level3 AS
    SELECT st.term_id, st.term_name, stl2.category_name FROM search_taxonomy_rels str, search_taxonomy st, search_taxonomy_level2 stl2 WHERE ((str.parent_id = stl2.term_id) AND (str.child_id = st.term_id));

