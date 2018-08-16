BEGIN;
DO LANGUAGE 'plpgsql' $$
DECLARE
cd_query varchar;
BEGIN
 cd_query := (select (string_agg('select ' || c_facttablecolumn || ' as concept_cd from ' || c_tablename || ' where ' || c_columnname || ' ' || c_operator || ' ''' || c_dimcode || ''' escape ''^'' ', ' union ') || ';') as query from i2b2metadata.i2b2_secure where c_visualattributes like '__D');
 set search_path TO "$user", i2b2demodata;
 EXECUTE format('CREATE TEMP TABLE tmp_concept_codes ON COMMIT DROP AS %s', cd_query);
 UPDATE observation_fact SET valtype_cd='D' WHERE concept_cd IN (select concept_cd from tmp_concept_codes);
END$$;
COMMIT; -- OR ROLLBACK