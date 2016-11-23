--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_TRUNCATE_RELEASE_TABLES
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_TRUNCATE_RELEASE_TABLES" 

AS
	--	Procedure to run one test in CZ_TEST

	--	JEA@20111019	New

	--	Define the abstract result set record

	TYPE r_type IS RECORD (
		rtn_text          VARCHAR2 (2000)
	);

	--	Define the abstract result set table
	TYPE tr_type IS TABLE OF r_type;

	--	Define the result set

	rtn_array tr_type;

	--	Variables

	tText 			varchar2(2000);

    --Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0);
	stepCt number(18,0);

	BEGIN

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := -1;

	SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
	procedureName := $$PLSQL_UNIT;

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		cz_start_audit (procedureName, databaseName, jobID);
	END IF;

	stepCt := 0;
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_truncate_release_tablese',0,stepCt,'Done');
	stepCt := stepCt + 1;

	tText := 'Select table_name from all_tables where owner = ' || '''' || 'TM_CZ' || '''' || 'and table_name like ' || '''' || '%_RELEASE' || '''';

	execute immediate(tText) BULK COLLECT INTO rtn_array;

	for i in rtn_array.first .. rtn_array.last
	loop
		dbms_output.put_line(rtn_array(i).rtn_text);

		if (rtn_array(i).rtn_text is not null) then
			tText := 'truncate table ' || rtn_array(i).rtn_text;

			execute immediate(tText);
			tText := 'Truncated ' || rtn_array(i).rtn_text;

			cz_write_audit(jobId,databaseName,procedureName,tText,0,stepCt,'Done');

		end if;

	end loop;

	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_truncate_release_tablese',0,stepCt,'Done');
	stepCt := stepCt + 1;

END;
/
 
