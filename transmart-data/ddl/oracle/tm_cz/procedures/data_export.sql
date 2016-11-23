--
-- Type: PROCEDURE; Owner: TM_CZ; Name: DATA_EXPORT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."DATA_EXPORT" 
AS
--Iterate through a cursor of all patient IDs
--Dynamically build a sql statement
--Run the statement returning the results

CURSOR cPatients is
  select distinct a.patient_num
    FROM observation_fact a
  join i2b2 b
    on a.concept_cd = b.c_basecode
  where c_fullname like '%BRC Depression Study%'
    and c_visualattributes not like '%H%'
  order by patient_num;

  dynamicSQL varchar2(32767);
  dynamicSQL2 varchar2(32767);

BEGIN
  dynamicSQL := 'select c_name ,c_fullname ';
  dynamicSQL2 := 'select c_name ,c_fullname ';

  FOR r_cPatients in cPatients Loop

    dynamicSQL  := dynamicSQL  || ',max(decode(patient_num,' || cast(r_cPatients.patient_num as varchar2) || ',tval_char,null)) "' || cast(r_cPatients.patient_num as varchar2) || '"';
    dynamicSQL2 := dynamicSQL2 || ',max(decode(patient_num,' || cast(r_cPatients.patient_num as varchar2) || ',cast(nval_num as varchar2(20)),null)) "' || cast(r_cPatients.patient_num as varchar2) || '"';

  END LOOP;

  dynamicSQL := dynamicSQL || ' from observation_fact a join i2b2 b on a.concept_cd = b.c_basecode where c_fullname like ''%BRC Depression Study%'' and c_columndatatype = ''T'' and c_visualattributes not like ''%H%'' group by c_name, c_fullname';
  dynamicSQL2 := dynamicSQL2 || ' from observation_fact a join i2b2 b on a.concept_cd = b.c_basecode where c_fullname like ''%BRC Depression Study%'' and c_columndatatype = ''N'' and c_visualattributes not like ''%H%'' group by c_name, c_fullname order by c_fullname';

  execute immediate(dynamicSQL || ' UNION ALL ' || dynamicsql2);

  dbms_output.put_line(dynamicSQL);-- || ' UNION ALL ' || dynamicsql2);
  dbms_output.put_line('UNION ALL');
  dbms_output.put_line(dynamicsql2);
END;


/
 
