--
-- Name: refresh_search_bio_mkr_correl_fast_mv(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION refresh_search_bio_mkr_correl_fast_mv() RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN

  -- populate the fake "materialized view" (i.e. actually a table) by deleting everything from it and then re-populating
  -- eventually we need a smarter algorithm for doing this

    delete from searchapp.search_bio_mkr_correl_fast_mv;

    insert into searchapp.search_bio_mkr_correl_fast_mv
    (domain_object_id, asso_bio_marker_id, correl_type, value_metric,  mv_id)    
    select domain_object_id, asso_bio_marker_id, correl_type, value_metric,  mv_id
       from searchapp.search_bio_mkr_correl_fast_view;  

    return true;
END;
 
 
 
$$;

