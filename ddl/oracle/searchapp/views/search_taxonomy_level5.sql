--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_TAXONOMY_LEVEL5
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_TAXONOMY_LEVEL5" ("TERM_ID", "TERM_NAME", "CATEGORY_NAME") AS 
  SELECT st.term_id,
  st.term_name,
  stl4.category_name
 FROM searchapp.search_taxonomy_rels str,
  search_taxonomy st,
  search_taxonomy_level4 stl4
WHERE ((str.parent_id = stl4.term_id) AND (str.child_id = st.term_id));
 
