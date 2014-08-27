--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_BULK_ADD_SEARCH_TERM
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_BULK_ADD_SEARCH_TERM" 
( 
  currentJobID NUMBER := null
)
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research null, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

	--Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0);
	stepCt number(18,0);

	v_keyword_term	varchar2(500);
  v_display_category varchar2(500);
  v_data_category varchar2(500);
  v_prefix varchar2(20);
  v_unique_ID varchar2(500);
  v_source_cd varchar2(500);
  v_parent_term varchar2(500);
  v_bio_data_id number(18,0);
  
	sqlText			varchar2(2000);
	Parent_Id 		Int;
	new_Term_Id 	Int;
	keyword_id 		int;
	Lcount 			Int; 
	Ncount 			Int;
  
	--v_category_display	varchar2(200);

  	type keyword_rec  is record
	(keywordName		varchar2(500),
   displayCategory varchar2(500),
   dataCategory varchar2(500),
   UIDPrefix varchar2(20),
   uniqueID varchar2(500),
   sourceCd varchar2(500),
   parentTerm varchar2(500),
   bio_data_id number(18,0)
	);

	type keyword_table is table of keyword_rec; 
	keyword_array keyword_table;
	
  
BEGIN

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
	procedureName := $$PLSQL_UNIT;

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		cz_start_audit (procedureName, databaseName, jobID);
	END IF;
    	
	--stepCt := 0;
  
	--cz_write_audit(jobId,databaseName,procedureName,'Start Procedure',SQL%ROWCOUNT,stepCt,'Done');
	--Stepct := Stepct + 1;	
  
	  select keyword, display_category, data_category, UID_prefix, unique_id, source_cd, parent_term, bio_data_id
    bulk collect into keyword_array
  	from tm_lz.lt_src_search_keyword
    where keyword is not null;

 
	for i in keyword_array.first .. keyword_array.last
	loop
		v_keyword_term := keyword_array(i).keywordName;
    v_display_category := keyword_array(i).displayCategory;
    v_data_category := keyword_array(i).dataCategory;
    v_prefix := keyword_array(i).UIDPrefix;
    v_unique_ID := keyword_array(i).uniqueID;
    v_source_cd := keyword_array(i).sourceCd;
    v_parent_term := keyword_array(i).parentTerm;
    v_bio_data_id := keyword_array(i).bio_data_id;
    
    --dbms_output.put_line('keyword: ' || v_keyword_term);


		if (v_display_category is null) then
			v_display_category := v_data_category;
    end if;


    if (v_unique_ID is null) then
      if (v_prefix is not null) then
        v_unique_ID := v_prefix || ':' || v_data_category || ':' || v_keyword_term;
      end if;
    end if;
   

		-- Insert taxonomy term into searchapp.search_keyword
		-- (searches Search_Keyword with the parent term to find the category to use)
		Insert Into Searchapp.Search_Keyword 
		(Data_Category
		,Keyword
		,Unique_Id
		,Source_Code
		,Display_Data_Category
    ,Bio_data_id)
		Select v_data_category
			  ,v_keyword_term
			  ,v_unique_ID
			  ,v_source_cd
			  ,v_display_category
        ,v_bio_data_id
		From dual
		where not exists
			(select 1 from searchapp.search_keyword x
			 where upper(x.data_category) = upper(v_data_category)
			   and upper(x.keyword) = upper(v_keyword_term)
         and upper(x.bio_data_id) = upper(v_bio_data_id));
		Cz_Write_Audit(Jobid,Databasename,Procedurename,v_keyword_term || ' added to Searchapp.Search_Keyword',Sql%Rowcount,Stepct,'Done');
		Stepct := Stepct + 1;	
		commit;
  
		-- Get the ID of the new term in Search_Keyword
		Select Search_Keyword_Id  Into keyword_Id 
		From  Searchapp.Search_Keyword 
		Where Upper(Keyword) = Upper(v_keyword_term)
		and upper(data_category) = upper(v_data_category)
    and upper(bio_data_id) = upper(v_bio_data_id);
		Cz_Write_Audit(Jobid,Databasename,Procedurename,'New search keyword ID stored in Keyword_Id',Sql%Rowcount,Stepct,'Done');
		Stepct := Stepct + 1;	
		
		-- Insert the new term into Searchapp.Search_v_keyword_term 
		insert into searchapp.search_keyword_term 
		(keyword_term
		,search_keyword_id
		,rank
		,term_length)
		select UPPER(v_keyword_term)
			  ,Keyword_Id
			  ,1
			  ,Length(v_keyword_term) 
		from dual
		where not exists
			(select 1 from searchapp.search_keyword_term x
			 where upper(x.keyword_term) = upper(v_keyword_term)
			   and x.search_keyword_id = Keyword_Id);
		Cz_Write_Audit(Jobid,Databasename,Procedurename,'Term added to Searchapp.Search_v_keyword_term',Sql%Rowcount,Stepct,'Done');
		Stepct := Stepct + 1;	
		commit;

	end loop;

	Cz_Write_Audit(Jobid,Databasename,Procedurename,'End '|| procedureName,0,Stepct,'Done');
	Stepct := Stepct + 1;  

 
     ---Cleanup OVERALL JOB if this proc is being run standalone    
  IF newJobFlag = 1
  THEN
    cz_end_audit (jobID, 'SUCCESS');
  END IF;
  
    EXCEPTION
  WHEN OTHERS THEN
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');
  
END;
/
 
