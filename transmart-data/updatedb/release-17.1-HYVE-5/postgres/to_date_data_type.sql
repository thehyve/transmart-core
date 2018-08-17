BEGIN;
DO LANGUAGE 'plpgsql' $$
DECLARE
cd_query varchar;
BEGIN
 cd_query := (select (string_agg('select concept_cd from i2b2demodata.concept_dimension where ' || c_columnname || ' ' || c_operator || ' ''' || c_dimcode || ''' escape ''^'' ', ' union ') || ';') as query from i2b2metadata.i2b2_secure where c_visualattributes like '__D' and UPPER(c_tablename) = 'CONCEPT_DIMENSION');
 set search_path TO "$user", i2b2demodata;
 EXECUTE format('CREATE TEMP TABLE tmp_concept_codes ON COMMIT DROP AS %s', cd_query);
 UPDATE observation_fact SET valtype_cd='D' WHERE concept_cd IN (select concept_cd from tmp_concept_codes) AND modifier_cd = '@' AND nval_num IS NOT NULL;
END$$;
COMMIT; -- OR ROLLBACK