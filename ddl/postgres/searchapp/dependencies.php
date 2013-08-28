<?php
$dependencies = array (
  'views/listsig_genes' => 
  array (
    0 => 'search_gene_signature',
    1 => 'search_gene_signature_item',
    2 => 'search_keyword',
  ),
  'views/search_auth_user_sec_access_v' => 
  array (
    0 => 'search_auth_group',
    1 => 'search_auth_group_member',
    2 => 'search_auth_sec_object_access',
    3 => 'search_auth_user',
  ),
  'views/search_bio_mkr_correl_fast_view' => 
  array (
    0 => 'search_gene_signature',
    1 => 'search_gene_signature_item',
  ),
  'views/search_categories' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy_rels',
  ),
  'views/search_taxonomy_level1' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy_rels',
    2 => 'views/search_categories',
  ),
  'views/search_taxonomy_level2' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy_rels',
    2 => 'views/search_taxonomy_level1',
  ),
  'views/search_taxonomy_level3' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy_rels',
    2 => 'views/search_taxonomy_level2',
  ),
  'views/search_taxonomy_level4' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy_rels',
    2 => 'views/search_taxonomy_level3',
  ),
  'views/search_taxonomy_level5' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy_rels',
    2 => 'views/search_taxonomy_level4',
  ),
  'views/search_taxonomy_lineage' => 
  array (
    0 => 'search_taxonomy_rels',
  ),
  'views/search_taxonomy_terms_cats' => 
  array (
    0 => 'views/search_taxonomy_level1',
    1 => 'views/search_taxonomy_level2',
    2 => 'views/search_taxonomy_level3',
    3 => 'views/search_taxonomy_level4',
    4 => 'views/search_taxonomy_level5',
  ),
  'views/solr_keywords_lineage' => 
  array (
    0 => 'search_taxonomy',
    1 => 'views/search_taxonomy_lineage',
  ),
  'search_taxonomy_rels' => 
  array (
    0 => 'search_taxonomy',
    1 => 'search_taxonomy',
  ),
  'search_taxonomy' => 
  array (
    0 => 'search_keyword',
  ),
  'search_role_auth_user' => 
  array (
    0 => 'search_auth_user',
    1 => 'search_role',
  ),
  'search_gene_signature' => 
  array (
    0 => 'search_auth_user',
    1 => 'search_gene_sig_file_schema',
    2 => 'search_auth_user',
  ),
  'plugin_module' => 
  array (
    0 => 'plugin',
  ),
  'saved_faceted_search' => 
  array (
    0 => 'search_auth_user',
  ),
  'search_auth_sec_object_access' => 
  array (
    0 => 'search_auth_principal',
    1 => 'search_sec_access_level',
    2 => 'search_secure_object',
  ),
  'search_auth_group_member' => 
  array (
    0 => 'search_auth_group',
    1 => 'search_auth_principal',
  ),
  'search_keyword_term' => 
  array (
    0 => 'search_keyword',
  ),
  'search_auth_user_sec_access' => 
  array (
    0 => 'search_auth_user',
    1 => 'search_sec_access_level',
    2 => 'search_secure_object',
  ),
  'search_auth_group' => 
  array (
    0 => 'search_auth_principal',
  ),
  'search_auth_user' => 
  array (
    0 => 'search_auth_principal',
  ),
)
;