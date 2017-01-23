--
-- Name: i2b2_truncate_release_tables(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_truncate_release_tables() RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE

	--	Procedure to run one test in CZ_TEST

	--	JEA@20111019	New

	--	Define the abstract result set record

	TYPE r_type IS RECORD (
		rtn_text          varchar(2000)
	);

	--	Define the abstract result set table
	TYPE tr_type IS TABLE OF r_type;

	--	Define the result set

	rtn_array tr_type;

	--	Variables

	tText 			varchar(2000);

    --Audit variables
	newJobFlag integer(1);
	databaseName varchar(100);
	procedureName varchar(100);
	jobID bigint;
	stepCt bigint;

	
BEGIN

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := -1;

	PERFORM sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName ;
	procedureName := $$PLSQL_UNIT;

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(coalesce(jobID::text, '') = '' or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		cz_start_audit (procedureName, databaseName, jobID);
	END IF;

	stepCt := 0;
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_truncate_release_tablese',0,stepCt,'Done');
	stepCt := stepCt + 1;

	tText := 'Select table_name from all_tables where owner = ' || '''' || 'TM_CZ' || '''' || 'and table_name like ' || '''' || '%_RELEASE' || '''';

	EXECUTE(tText) BULK COLLECT INTO rtn_array;

	for i in rtn_array.first .. rtn_array.last
	loop
		RAISE NOTICE '%', rtn_array(i).rtn_text;

		if (rtn_array(i)(.rtn_text IS NOT NULL AND .rtn_text::text <> '')) then
			tText := 'truncate table ' || rtn_array(i).rtn_text;

			EXECUTE(tText);
			tText := 'Truncated ' || rtn_array(i).rtn_text;

			cz_write_audit(jobId,databaseName,procedureName,tText,0,stepCt,'Done');

		end if;

	end loop;

	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_truncate_release_tablese',0,stepCt,'Done');
	stepCt := stepCt + 1;

END;
 
$_$;

