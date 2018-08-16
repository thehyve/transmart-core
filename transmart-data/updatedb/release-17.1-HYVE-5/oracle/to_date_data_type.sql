DECLARE
BEGIN
 FOR r IN (select LISTAGG('select ' || c_facttablecolumn || ' as concept_cd from i2b2demodata.' || c_tablename || ' where ' || c_columnname || ' ' || c_operator || ' ''' || c_dimcode || ''' escape ''^'' ', ' union ') WITHIN GROUP (ORDER BY c_facttablecolumn, c_tablename, c_columnname, c_operator, c_dimcode) as query from i2b2metadata.i2b2_secure where c_visualattributes like '__D' group by c_facttablecolumn, c_tablename, c_columnname, c_operator, c_dimcode) LOOP
  EXECUTE IMMEDIATE 'UPDATE i2b2demodata.observation_fact SET valtype_cd=''D'' WHERE concept_cd IN (' || r.query || ')';
 END LOOP;
END;