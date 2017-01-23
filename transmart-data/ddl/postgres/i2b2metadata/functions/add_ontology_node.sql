--
-- Name: add_ontology_node(character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: i2b2metadata; Owner: -
--
CREATE FUNCTION add_ontology_node(parent_path_src character varying, node_name character varying, is_leaf_src character varying, is_number character varying, prefix character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  declare
    max_basecode numeric;
    parent_path varchar(255);
    is_leaf varchar(30);
    ct numeric;
    hlevel numeric;
    concept_code varchar(30);
    xml varchar(1000);
    data_type varchar(30);
    sourcesyscd varchar(30);
  begin
  
------------------------------------------------------------
-- get parent path
------------------------------------------------------------  
    --parent_path := '&parent_path';
    
      select count(*) into ct
      from i2b2metadata.i2b2
      where c_fullname=parent_path_src;
      
      if (ct=0) then
        RAISE NOTICE 'Supplied path: % does not exist', parent_path_src;
        return;
      end if;
      
    if (substring(parent_path from length(parent_path) for 1)!='\') then
      parent_path := parent_path_src||'\';
    else parent_path := parent_path_src;
    end if;
   
    sourcesyscd := substr(parent_path, 2, instr(parent_path, '\', 2)-2);
    RAISE NOTICE 'parent path: %, source system code: %', parent_path,  sourcesyscd;
------------------------------------------------------------
-- get the name of the node
------------------------------------------------------------
  --node_name := '&node_name';
    RAISE NOTICE 'node name: %', node_name;
------------------------------------------------------------
-- find out whether the node supplied is a leaf or a folder
------------------------------------------------------------
  --is_leaf := '&IsLeaf_Y_N';
  ----------------------------------------------------------
  -- if node is a leaf, set attribute value to LA, set 
  -- concept code and find out what data type the leaf is
  ----------------------------------------------------------
    is_leaf := is_leaf_src;
    
    if (upper(is_leaf) = 'Y') then
      
      is_leaf := 'LA';
      
      PERFORM prefix||nextval('seq_i2b2_data_id')
      into concept_code ;
      
      --is_number :='&is_number';
      if (upper(is_number) = 'Y') then
        xml := '<?xml version="1.0"?><ValueMetadata><Version>3.02</Version><CreationDateTime>08/14/2008 01:22:59</CreationDateTime><TestID></TestID><TestName></TestName><DataType>PosFloat</DataType><CodeType></CodeType><Loinc></Loinc><Flagstouse></Flagstouse><Oktousevalues>Y</Oktousevalues><MaxStringLength></MaxStringLength><LowofLowValue>0</LowofLowValue><HighofLowValue>0</HighofLowValue><LowofHighValue>100</LowofHighValue>100<HighofHighValue>100</HighofHighValue><LowofToxicValue></LowofToxicValue><HighofToxicValue></HighofToxicValue><EnumValues></EnumValues><CommentsDeterminingExclusion><Com></Com></CommentsDeterminingExclusion><UnitValues><NormalUnits>ratio</NormalUnits><EqualUnits></EqualUnits><ExcludingUnits></ExcludingUnits><ConvertingUnits><Units></Units><MultiplyingFactor></MultiplyingFactor></ConvertingUnits></UnitValues><Analysis><Enums /><Counts /><New /></Analysis></ValueMetadata>';
        data_type := 'N';
      elsif (upper(is_number) = 'N') then
        data_type := 'T';
      else 
        RAISE NOTICE 'IS_NUMBER prompt requires ''Y'' or ''N'' input';
        return;
      end if;
    elsif (upper(is_leaf) ='N') then 
      is_leaf := 'FA';
      data_type := 'T';
    else 
      RAISE NOTICE 'IS_LEAF prompt requires ''Y'' or ''N'' input';
      return;
    end if;
    RAISE NOTICE 'node type: %, data type: %, concept code: %', is_leaf, data_type, concept_code;
  
------------------------------------------------------------
-- obtail the proper level for the node by finding the 
-- level of its parent and adding one.
------------------------------------------------------------
    select c_hlevel+1 into hlevel
    from i2b2metadata.i2b2
    where upper(c_fullname)=upper(parent_path);
    RAISE NOTICE 'level: %', hlevel;
------------------------------------------------------------
-- add the new node into the i2b2 table
------------------------------------------------------------
    insert into i2b2demodata.i2b2(
    C_HLEVEL, C_FULLNAME, C_NAME
    , C_SYNONYM_CD, C_VISUALATTRIBUTES, C_TOTALNUM
    , C_BASECODE, C_METADATAXML, C_FACTTABLECOLUMN
    , C_TABLENAME, C_COLUMNNAME, C_COLUMNDATATYPE
    , C_OPERATOR, C_DIMCODE, C_COMMENT
    , C_TOOLTIP, UPDATE_DATE, DOWNLOAD_DATE
    , IMPORT_DATE, SOURCESYSTEM_CD, VALUETYPE_CD 
    )
    values (hlevel, parent_path||node_name||'\', node_name
    , 'N', is_leaf, 0
    , concept_code, xml, 'concept_cd'
    , 'concept_dimension', 'concept_path', data_type
    , 'LIKE', parent_path||node_name||'\', null
    , parent_path||node_name, LOCALTIMESTAMP, LOCALTIMESTAMP
    , LOCALTIMESTAMP, sourcesyscd, null);
    commit;
  end;
end;
 
$$;

