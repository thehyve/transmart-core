--
-- Type: VIEW; Owner: SEARCHAPP; Name: SEARCH_CATEGORIES
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SEARCH_CATEGORIES" ("CATEGORY_ID", "CATEGORY_NAME") AS 
  SELECT str.child_id AS category_id,
  st.term_name AS category_name
 FROM searchapp.search_taxonomy_rels str,
  searchapp.search_taxonomy st
WHERE ((str.parent_id = ( SELECT search_taxonomy_rels.child_id
         FROM search_taxonomy_rels
WHERE (search_taxonomy_rels.parent_id IS NULL))) AND (str.child_id = st.term_id));
 
