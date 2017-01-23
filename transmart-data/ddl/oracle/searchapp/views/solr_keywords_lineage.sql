--
-- Type: VIEW; Owner: SEARCHAPP; Name: SOLR_KEYWORDS_LINEAGE
--
  CREATE OR REPLACE FORCE VIEW "SEARCHAPP"."SOLR_KEYWORDS_LINEAGE" ("TERM_ID", "ANCESTOR_ID", "SEARCH_KEYWORD_ID") AS 
  SELECT DISTINCT union_results.term_id,
  union_results.ancestor_id,
  union_results.search_keyword_id
 FROM (        (        (        (         SELECT DISTINCT l.child_id AS term_id,
                                          l.child_id AS ancestor_id,
                                          st.search_keyword_id
                                         FROM searchapp.search_taxonomy_lineage l,
                                          searchapp.search_taxonomy st
                                        WHERE ((l.child_id = st.term_id) AND (l.child_id IS NOT NULL))
                              UNION
                                       SELECT DISTINCT l.child_id AS term_id,
                                          l.parent1 AS ancestor_id,
                                          st.search_keyword_id
                                         FROM searchapp.search_taxonomy_lineage l,
                                          searchapp.search_taxonomy st
                                        WHERE ((l.parent1 = st.term_id) AND (l.parent1 IS NOT NULL)))
                      UNION
                               SELECT DISTINCT l.child_id AS term_id,
                                  l.parent2 AS ancestor_id,
                                  st.search_keyword_id
                                 FROM searchapp.search_taxonomy_lineage l,
                                  searchapp.search_taxonomy st
                                WHERE ((l.parent2 = st.term_id) AND (l.parent2 IS NOT NULL)))
              UNION
                       SELECT DISTINCT l.child_id AS term_id,
                          l.parent3 AS ancestor_id,
                          st.search_keyword_id
                         FROM searchapp.search_taxonomy_lineage l,
                          searchapp.search_taxonomy st
                        WHERE ((l.parent3 = st.term_id) AND (l.parent3 IS NOT NULL)))
      UNION
               SELECT DISTINCT l.child_id AS term_id,
                  l.parent4 AS ancestor_id,
                  st.search_keyword_id
                 FROM searchapp.search_taxonomy_lineage l,
                  searchapp.search_taxonomy st
                WHERE ((l.parent4 = st.term_id) AND (l.parent4 IS NOT NULL))) union_results
WHERE (union_results.search_keyword_id IS NOT NULL);
 
