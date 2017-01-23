--
-- Name: search_taxonomy_lineage; Type: VIEW; Schema: searchapp; Owner: -
--
CREATE VIEW search_taxonomy_lineage AS
    SELECT s1.child_id, s2.child_id AS parent1, s3.child_id AS parent2, s4.child_id AS parent3, s5.child_id AS parent4 FROM search_taxonomy_rels s1, search_taxonomy_rels s2, search_taxonomy_rels s3, search_taxonomy_rels s4, search_taxonomy_rels s5 WHERE ((((s1.parent_id = s2.child_id) AND (s2.parent_id = s3.child_id)) AND (s3.parent_id = s4.child_id)) AND (s4.parent_id = s5.child_id));

