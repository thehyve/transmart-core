--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UTIL_IS_POPULATED
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UTIL_IS_POPULATED" 
(
  tabname IN varchar2,
  retval OUT integer
)
AUTHID CURRENT_USER
as
-------------------------------------------------------------------------------------
-- NAME: UTIL_IS_POPULATED
--
-- Copyright c 2011 Recombinant Data Corp.
--

--------------------------------------------------------------------------------------
  sqltext varchar2(500);
  l_count pls_integer;

begin

   sqltext := 'select count(*) into :result from ' || tabname;

   execute immediate sqltext into l_count;


   if l_count > 0 then
   retval :=1;
   else
    retval := 0;
   end if;

   --dbms_output.put_line(l_count);

end;
/
 
