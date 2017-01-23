--
-- Type: PROCEDURE; Owner: I2B2DEMODATA; Name: REMOVE_TEMP_TABLE
--
  CREATE OR REPLACE PROCEDURE "I2B2DEMODATA"."REMOVE_TEMP_TABLE" (tempTableName VARCHAR)
IS
BEGIN
	execute immediate 'drop table ' || tempTableName || ' cascade constraints';

EXCEPTION
	WHEN OTHERS THEN
		dbms_output.put_line(SQLCODE|| ' - ' ||SQLERRM);
END;
/
 
