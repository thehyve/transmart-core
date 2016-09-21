--
-- Type: VIEW; Owner: TM_CZ; Name: CZV_PIVOT_SAMPLE_CATEGORIES
--
  CREATE OR REPLACE FORCE VIEW "TM_CZ"."CZV_PIVOT_SAMPLE_CATEGORIES" ("TRIAL_CD", "SAMPLE_CD", "TRIAL_NAME", "PATHOLOGY", "RACE", "TISSUE_TYPE", "GENDER", "BIOMARKER", "ACCESS_TYPE", "INSTITUTION", "PROGRAM_INITIATIVE", "ORGANISM", "COMPOUND", "PHASE") AS 
  select x.trial_cd
      ,x.sample_cd
	  ,x.trial_name
	  ,coalesce(x.pathology,'Not Applicable') as pathology
	  ,coalesce(x.race,'Not Applicable') as race
	  ,coalesce(x.tissue_type,'Not Applicable') as tissue_type
	  ,coalesce(x.gender,'Not Applicable') as gender
	  ,coalesce(x.biomarker,'Not Applicable') as biomarker
	  ,coalesce(x.access_type,'Not Applicable') as biomarker
	  ,coalesce(x.institution,'Not Applicable') as institution
	  ,coalesce(x.program_initiative,'Not Applicable') as program_initiative
	  ,coalesce(x.organism,'Not Applicable') as organism
	  ,coalesce(x.compound,'Not Applicable') as compound
	  ,coalesce(x.phase,'Not Applicable') as phase
from 
	(select s.trial_cd
		   ,s.sample_cd
		   --,p.sourcesystem_cd as sample_cd
		   --,to_char(p.patient_num) as sample_cd
		   ,f.c_name as trial_name
		   ,max(decode(s.category_cd,'PATHOLOGY',s.category_value,null)) as PATHOLOGY
		   ,max(decode(s.category_cd,'RACE',s.category_value,null)) as RACE
		   ,max(decode(s.category_cd,'TISSUE_TYPE',s.category_value,null)) as TISSUE_TYPE
		   ,max(decode(s.category_cd,'GENDER',s.category_value,null)) as GENDER
		   ,max(decode(s.category_cd,'BIOMARKER',s.category_value,null)) as BIOMARKER
		   ,max(decode(s.category_cd,'ACCESS',s.category_value,null)) as ACCESS_TYPE
		   ,max(decode(s.category_cd,'INSTITUTION',s.category_value,null)) as INSTITUTION
		   ,max(decode(s.category_cd,'PROGRAM/INITIATIVE',s.category_value,null)) as PROGRAM_INITIATIVE
		   ,max(decode(s.category_cd,'ORGANISM',s.category_value,null)) as ORGANISM
		   ,max(decode(s.category_cd,'COMPOUND',s.category_value,null)) as COMPOUND
		   ,max(decode(s.category_cd,'PHASE',s.category_value,null)) as PHASE
	 from lz_src_sample_categories s
		,i2b2 f		
		--,patient_dimension p
	 where s.trial_cd = f.sourcesystem_cd
	   and f.c_hlevel = (select min(x.c_hlevel) from i2b2 x
                         where f.sourcesystem_cd = x.sourcesystem_cd
						)
	   --and f.c_hlevel = 0 
	   --and p.sourcesystem_cd = 
		--	case when s.sample_cd is null 
		--		 then regexp_replace(s.trial_cd || ':' || s.site_cd || ':' || s.subject_cd,'(::){1,}', ':')
		--	     else regexp_replace(s.trial_cd || ':S:' || s.site_cd || ':' || s.subject_cd || ':' || s.sample_cd,'(::){1,}', ':')
		--	end
	group by s.trial_cd
			,s.sample_cd
			--,p.sourcesystem_cd
			--,p.patient_num
			,f.c_name 
) x;
 
--
-- Type: INDEX; Owner: TM_CZ; Name: IDX_I2B2_SECURE_FULLNAME
--
CREATE INDEX "TM_CZ"."IDX_I2B2_SECURE_FULLNAME" ON "I2B2METADATA"."I2B2_SECURE" ("C_FULLNAME")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: TM_CZ; Name: IX_I2B2_SOURCE_SYSTEM_CD
--
CREATE INDEX "TM_CZ"."IX_I2B2_SOURCE_SYSTEM_CD" ON "I2B2METADATA"."I2B2" ("SOURCESYSTEM_CD")
TABLESPACE "TRANSMART" ;

--
-- Type: INDEX; Owner: TM_CZ; Name: IX_DE_SUBJECT_SMPL_MPNG_MRNA
--
CREATE INDEX "TM_CZ"."IX_DE_SUBJECT_SMPL_MPNG_MRNA" ON "DEAPP"."DE_SUBJECT_SAMPLE_MAPPING" ("TRIAL_NAME", "PLATFORM", "SOURCE_CD", "CONCEPT_CODE")
TABLESPACE "TRANSMART" ;

