--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_TAXONOMY_LEVEL3
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_TAXONOMY_LEVEL3" ("TERM_ID", "TERM_NAME", "CATEGORY_NAME") AS 
  SELECT st.term_id,
  st.term_name,
  stl2.category_name
 FROM searchapp.search_taxonomy_rels str,
  search_taxonomy st,
  search_taxonomy_level2 stl2
WHERE ((str.parent_id = stl2.term_id) AND (str.child_id = st.term_id));
 
