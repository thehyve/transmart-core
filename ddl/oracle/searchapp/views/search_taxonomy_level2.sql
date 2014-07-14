--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_TAXONOMY_LEVEL2
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_TAXONOMY_LEVEL2" ("TERM_ID", "TERM_NAME", "CATEGORY_NAME") AS 
  SELECT st.term_id,
  st.term_name,
  stl1.category_name
 FROM searchapp.search_taxonomy_rels str,
  search_taxonomy st,
  search_taxonomy_level1 stl1
WHERE ((str.parent_id = stl1.term_id) AND (str.child_id = st.term_id));
 
