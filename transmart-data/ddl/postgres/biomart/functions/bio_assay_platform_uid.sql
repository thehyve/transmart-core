--
-- Name: bio_assay_platform_uid(character varying); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION bio_assay_platform_uid(platform_name character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $_$
BEGIN
  -- $Id$
  -- Creates uid for bio_assay_platform

  RETURN 'BAP:' || coalesce(PLATFORM_NAME, 'ERROR');
END;
$_$;

