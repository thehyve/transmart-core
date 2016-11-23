--
-- Name: data_export(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION data_export() RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

--Iterate through a cursor of all patient IDs
--Dynamically build a sql statement
--Run the statement returning the results

cPatients CURSOR FOR
  SELECT distinct a.patient_num
    FROM observation_fact a
  join i2b2 b
    on a.concept_cd = b.c_basecode
  where c_fullname like '%BRC Depression Study%'
    and c_visualattributes not like '%H%'
  order by patient_num;

  dynamicSQL varchar(32767);
  dynamicSQL2 varchar(32767);


BEGIN
  dynamicSQL := 'select c_name ,c_fullname ';
  dynamicSQL2 := 'select c_name ,c_fullname ';

  FOR r_cPatients in cPatients Loop

    dynamicSQL  := dynamicSQL  || ',max(decode(patient_num,' || cast(r_cPatients.patient_num as varchar) || ',tval_char,null)) "' || cast(r_cPatients.patient_num as varchar) || '"';
    dynamicSQL2 := dynamicSQL2 || ',max(decode(patient_num,' || cast(r_cPatients.patient_num as varchar) || ',cast(nval_num as varchar(20)),null)) "' || cast(r_cPatients.patient_num as varchar) || '"';

  END LOOP;

  dynamicSQL := dynamicSQL || ' from observation_fact a join i2b2 b on a.concept_cd = b.c_basecode where c_fullname like ''%BRC Depression Study%'' and c_columndatatype = ''T'' and c_visualattributes not like ''%H%'' group by c_name, c_fullname';
  dynamicSQL2 := dynamicSQL2 || ' from observation_fact a join i2b2 b on a.concept_cd = b.c_basecode where c_fullname like ''%BRC Depression Study%'' and c_columndatatype = ''N'' and c_visualattributes not like ''%H%'' group by c_name, c_fullname order by c_fullname';

  EXECUTE(dynamicSQL || ' UNION ALL ' || dynamicsql2);

  RAISE NOTICE '%', dynamicSQL;-- || ' UNION ALL ' || dynamicsql2);
  RAISE NOTICE 'UNION ALL';
  RAISE NOTICE '%', dynamicsql2;
END;
 
$$;

