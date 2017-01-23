--
-- Name: search_taxonomy_level4; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_level4 AS
    SELECT st.term_id, st.term_name, stl3.category_name FROM search_taxonomy_rels str, search_taxonomy st, search_taxonomy_level3 stl3 WHERE ((str.parent_id = stl3.term_id) AND (str.child_id = st.term_id));

