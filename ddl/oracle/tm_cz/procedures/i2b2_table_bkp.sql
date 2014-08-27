--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_TABLE_BKP
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_TABLE_BKP" 
AS

	tExists		int;
BEGIN
/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
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
  
	select count(*) into tExists
	from all_tables
	where owner = 'I2B2METADATA'
      and table_name = 'I2B2_BKP';
	
	if tExists > 0 then
		execute immediate('drop table i2b2metadata.i2b2_bkp');
	end if;
	
	select count(*) into tExists
	from all_tables
	where owner = 'I2B2DEMODATA'
      and table_name = 'CONCEPT_COUNTS_BKP';
	
	if tExists > 0 then	
		execute immediate('drop table i2b2demodata.concept_counts_bkp');
	end if;
  
	select count(*) into tExists
	from all_tables
	where owner = 'I2B2DEMODATA'
      and table_name = 'CONCEPT_DIMENSION_BKP';
	
	if tExists > 0 then
		execute immediate('drop table i2b2demodata.concept_dimension_bkp');
	end if;
  
 	select count(*) into tExists
	from all_tables
	where owner = 'I2B2DEMODATA'
      and table_name = 'OBSERVATION_FACT_BKP';
	
	if tExists > 0 then
		execute immediate('drop table i2b2demodata.observation_fact_bkp');
	end if;
  
 	select count(*) into tExists
	from all_tables
	where owner = 'I2B2DEMODATA'
      and table_name = 'PATIENT_DIMENSION_BKP';
	
	if tExists > 0 then
		execute immediate('drop table i2b2demodata.patient_dimension_bkp');
	end if;

	--Backup tables
	EXECUTE IMMEDIATE 'CREATE TABLE I2B2METADATA.I2B2_BKP AS SELECT * FROM I2B2METADATA.I2B2';
	EXECUTE IMMEDIATE 'CREATE TABLE I2B2DEMODATA.CONCEPT_COUNTS_BKP AS SELECT * FROM I2B2DEMODATA.CONCEPT_COUNTS';
	EXECUTE IMMEDIATE 'CREATE TABLE I2B2DEMODATA.CONCEPT_DIMENSION_BKP AS SELECT * FROM I2B2DEMODATA.CONCEPT_DIMENSION';
	EXECUTE IMMEDIATE 'CREATE TABLE I2B2DEMODATA.OBSERVATION_FACT_BKP AS SELECT * FROM I2B2DEMODATA.OBSERVATION_FACT';
	EXECUTE IMMEDIATE 'CREATE TABLE I2B2DEMODATA.PATIENT_DIMENSION_BKP AS SELECT * FROM I2B2DEMODATA.PATIENT_DIMENSION';
END;
/
 
