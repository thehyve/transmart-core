--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_TAXONOMY_LEVEL1
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_TAXONOMY_LEVEL1" ("TERM_ID", "TERM_NAME", "CATEGORY_NAME") AS 
  SELECT st.term_id,
  st.term_name,
  sc.category_name
 FROM searchapp.search_taxonomy_rels str,
  search_taxonomy st,
  search_categories sc
WHERE ((str.parent_id = sc.category_id) AND (str.child_id = st.term_id));
 
