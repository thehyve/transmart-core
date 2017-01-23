--
-- Name: search_categories; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_categories AS
    SELECT str.child_id AS category_id, st.term_name AS category_name FROM search_taxonomy_rels str, search_taxonomy st WHERE ((str.parent_id = (SELECT search_taxonomy_rels.child_id FROM search_taxonomy_rels WHERE (search_taxonomy_rels.parent_id IS NULL))) AND (str.child_id = st.term_id));

