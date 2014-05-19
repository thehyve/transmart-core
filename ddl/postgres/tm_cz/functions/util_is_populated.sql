--
-- Name: util_is_populated(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION util_is_populated (
  tabname IN character varying,
  retval OUT integer
)
--AUTHID CURRENT_USER
 RETURNS integer AS $body$
DECLARE

-------------------------------------------------------------------------------------
-- NAME: UTIL_IS_POPULATED
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
  sqltext varchar(500);
  l_count integer;


BEGIN

   sqltext := 'select count(*) into result from ' || tabname;

   EXECUTE sqltext into l_count;


   if l_count > 0 then
   retval :=1;
   else
    retval := 0;
   end if;

   --dbms_output.put_line(l_count);

end;
 
$body$
LANGUAGE PLPGSQL;
