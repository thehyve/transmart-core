--
-- Name: bio_compound(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION bio_compound_uid ( JNJ_NUMBER text
)  RETURNS varchar AS $body$
BEGIN
  -- $Id$
  -- Function to create compound_uid.

  RETURN 'COM:' || JNJ_NUMBER;
END BIO_COMPOUND_UID;
 
$body$
LANGUAGE PLPGSQL;
