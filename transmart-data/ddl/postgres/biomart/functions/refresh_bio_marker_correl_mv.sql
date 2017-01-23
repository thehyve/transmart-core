--
-- Name: refresh_bio_marker_correl_mv(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION refresh_bio_marker_correl_mv() RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN

  -- populate the fake "materialized view" (i.e. actually a table) by deleting everything from it and then re-populating
  -- eventually we need a smarter algorithm for doing this

    delete from biomart.bio_marker_correl_mv;

    insert into biomart.bio_marker_correl_mv
    (BIO_MARKER_ID, ASSO_BIO_MARKER_ID, CORREL_TYPE, mv_id)    
    select BIO_MARKER_ID, ASSO_BIO_MARKER_ID, CORREL_TYPE, mv_id
       from biomart.bio_marker_correl_view;  

    return true;
END;
$$;

