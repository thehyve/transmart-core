--
-- Name: refresh_bio_lit_int_model_mv(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION refresh_bio_lit_int_model_mv() RETURNS character varying
    LANGUAGE plpgsql
    AS $$
BEGIN

  -- populate the fake "materialized view" (i.e. actually a table) by deleting everything from it and then re-populating
  -- eventually we need a smarter algorithm for doing this

    delete from biomart.bio_lit_int_model_mv;

    insert into biomart.bio_lit_int_model_mv
    (bio_lit_int_data_id, experimental_model)    
    select bio_lit_int_data_id, experimental_model
       from biomart.bio_lit_int_model_view;  

    return true;
END;
 
 
 
$$;

