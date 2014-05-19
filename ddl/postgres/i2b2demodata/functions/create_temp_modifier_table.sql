--
-- Name: create_temp_modifier_table(character varying, character varying); Type: FUNCTION; Schema: i2b2demodata; Owner: -
--
CREATE OR REPLACE FUNCTION create_temp_modifier_table (tempModifierTableName IN character varying,
  errorMsg OUT character varying)
 RETURNS character varying AS $body$
BEGIN
EXECUTE 'create table ' ||  tempModifierTableName || ' (
        MODIFIER_CD varchar(50) NOT NULL,
	MODIFIER_PATH varchar(900) NOT NULL ,
	NAME_CHAR varchar(2000),
	MODIFIER_BLOB text,
	UPDATE_DATE timestamp,
	DOWNLOAD_DATE timestamp,
	IMPORT_DATE timestamp,
	SOURCESYSTEM_CD varchar(50)
	 )';

 EXECUTE 'CREATE INDEX idx_' || tempModifierTableName || '_pat_id ON ' || tempModifierTableName || '  (MODIFIER_PATH)';

EXCEPTION
	WHEN OTHERS THEN
		RAISE NOTICE '%%%', SQLSTATE,  ' - ' , SQLERRM;
END;
 
$body$
LANGUAGE PLPGSQL;
