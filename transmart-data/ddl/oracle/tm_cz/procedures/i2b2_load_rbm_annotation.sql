--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_RBM_ANNOTATION
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_RBM_ANNOTATION" 
(
currentJobID NUMBER := null
 )
AS
/*************************************************************************
* This is for RBM Annotation ETL for Sanofi
* Date:12/05/2013
******************************************************************/

	--Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0);
	stepCt number(18,0);
	gplId	varchar2(100);

BEGIN

	stepCt := 0;

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

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_rbm_annotation',0,stepCt,'Done');

	--	get GPL id from external table
	
	select distinct gpl_id into gplId from LT_SRC_RBM_ANNOTATION;
	
	
	--	delete any existing data from antigen_deapp
	
	delete from antigen_deapp
	where platform = gplId;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from REFERENCE antigen_deapp',SQL%ROWCOUNT,stepCt,'Done');

		
	--	delete any existing data from annotation_deapp
	
	delete from annotation_deapp
	where gpl_id = gplId;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from annotation_deapp',SQL%ROWCOUNT,stepCt,'Done');

	--	delete any existing data from deapp.de_mrna_annotation
	
	delete from deapp.DE_RBM_ANNOTATION
	where gpl_id = gplId;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_mrna_annotation',SQL%ROWCOUNT,stepCt,'Done');

	--	update organism for existing probesets in probeset_deapp
	/*
	update probeset_deapp p  --check
	set organism=(select distinct t.organism from LT_SRC_RBM_ANNOTATION t
				  where p.platform = t.gpl_id
				    and p.probeset = t.probe_id)
	where exists
		 (select 1 from LT_SRC_RBM_ANNOTATION x
		  where p.platform = x.gpl_id
		    and p.probeset = x.probe_id);
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update organism in probeset_deapp',SQL%ROWCOUNT,stepCt,'Done');
		*/	
	--	insert any new probesets into probeset_deapp
	
	insert into antigen_deapp 
	(antigen_name
	,platform)
	select distinct antigen_name
	      ,gpl_id
	from LT_SRC_RBM_ANNOTATION t
	where not exists
		 (select 1 from antigen_deapp x
		  where t.gpl_id = x.platform
		    and t.antigen_name = x.antigen_name
		);
	--where gene_id is not null 
	--   or gene_symbol is not null;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert new probesets into antigen_deapp',SQL%ROWCOUNT,stepCt,'Done');
		
	--	insert data into annotation_deapp
	
	insert into annotation_deapp
	(gpl_id
	,probe_id
	,gene_symbol
	,gene_id
	,probeset_id
	,organism)
	select distinct d.gpl_id
	,d.uniprotid
	,d.gene_symbol
	,d.gene_id
	,p.antigen_id
	,'Homo sapiens'
	from LT_SRC_RBM_ANNOTATION d
	,antigen_deapp p
	where d.antigen_name = p.antigen_name
	  and d.gpl_id = p.platform
	  and (d.gene_id is not null or d.gene_symbol is not null) ;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into REFERENCE annotation_deapp',SQL%ROWCOUNT,stepCt,'Done');
		
	--	insert data into deapp.de_rbm_annotation
	
	insert into de_rbm_annotation
	(gpl_id
        --,id
	,antigen_name
        ,uniprot_id
	,gene_symbol
	,gene_id
	) 
        SELECT d.gpl_id
        --,p.antigen_id
        ,d.antigen_name,trim(d.uniprotid) as uniprotid,d.gene_symbol,decode(d.gene_id,null,null,to_number(d.gene_id)) as gene_id FROM (
        SELECT DISTINCT REGEXP_SUBSTR (uniprotid, '[^,]+', 1, RN) AS uniprotid,antigen_name,gene_id,gpl_id,gene_symbol
        FROM TM_LZ.LT_SRC_RBM_ANNOTATION 
        CROSS JOIN (SELECT ROWNUM AS RN 
        FROM (SELECT MAX(REGEXP_COUNT(uniprotid,',') + 1) MX FROM TM_LZ.LT_SRC_RBM_ANNOTATION) 
        CONNECT BY LEVEL <= MX) 
        WHERE REGEXP_SUBSTR (uniprotid, '[^,]+', 1, RN) IS NOT NULL 
        ORDER BY 1
        )d
        ,antigen_deapp p 
	where d.antigen_name = p.antigen_name
        and d.gpl_id = p.platform
        union       ----Changes made to solve UAT 142 on 27/02/2014 -- populate antigens without uniprot_id 
        SELECT gpl_id,antigen_name,uniprotid,gene_symbol,decode(gene_id,null,null,to_number(gene_id)) as gene_id  FROM TM_LZ.LT_SRC_RBM_ANNOTATION WHERE UNIPROTID IS NULL;        
        
	commit;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_rbm_annotation',SQL%ROWCOUNT,stepCt,'Done');
		
        update DEAPP.DE_RBM_ANNOTATION set uniprot_name = (select bio_marker_name from BIOMART.BIO_MARKER
        WHERE biomart.bio_marker.primary_external_id = deapp.de_rbm_annotation.uniprot_id)
        where gpl_id = gplId;  
        
        commit;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update uniprot_name in DEAPP de_rbm_annotation',SQL%ROWCOUNT,stepCt,'Done');
		
                
	--	update gene_id if null
	
	update de_rbm_annotation t
	set gene_id=(select to_number(min(b.primary_external_id)) as gene_id
				 from biomart.bio_marker b
				 where t.gene_symbol = b.bio_marker_name
				  -- and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'RBM')
	where t.gpl_id = gplId
	  and t.gene_id is null
	  and t.gene_symbol is not null
	  and exists
		 (select 1 from biomart.bio_marker x
		  where t.gene_symbol = x.bio_marker_name
			--and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'RBM');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing gene_id in de_rbm_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
	--	update gene_symbol if null
	
	update de_rbm_annotation t 
	set gene_symbol=(select min(b.bio_marker_name) as gene_symbol
				 from biomart.bio_marker b
				 where to_char(t.gene_id) = b.primary_external_id
				 --  and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'RBM')
	where t.gpl_id = gplId
	  and t.gene_symbol is null
	  and t.gene_id is not null
	  and exists
		 (select 1 from biomart.bio_marker x
		  where to_char(t.gene_id) = x.primary_external_id
			--and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'RBM');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing gene_id in de_rbm_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
	--	insert probesets into biomart.bio_assay_feature_group
	
	insert into biomart.bio_assay_feature_group
	(feature_group_name
	,feature_group_type)
	select distinct t.uniprotid, 'RBM'
	from tm_lz.LT_SRC_RBM_ANNOTATION t
	where not exists
		 (select 1 from biomart.bio_assay_feature_group x
		  where t.uniprotid = x.feature_group_name)
		and t.uniprotid is not null;
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert probesets into biomart.bio_assay_feature_group',SQL%ROWCOUNT,stepCt,'Done');
		  
	--	insert probesets into biomart.bio_assay_data_annotation
	
	insert into biomart.bio_assay_data_annotation
	(bio_assay_feature_group_id
	,bio_marker_id)
	select distinct fg.bio_assay_feature_group_id
		  ,coalesce(bgs.bio_marker_id,bgi.bio_marker_id)
	from LT_SRC_RBM_ANNOTATION t
		,biomart.bio_assay_feature_group fg
		,biomart.bio_marker bgs
		,biomart.bio_marker bgi
	where (t.gene_symbol is not null or t.gene_id is not null)
	  and t.uniprotid = fg.feature_group_name
	  and t.gene_symbol = bgs.bio_marker_name(+)
	--  and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgs.organism)
	  and to_char(t.gene_id) = bgi.primary_external_id(+)
	 -- and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgi.organism)
	  and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) > 0
	  and not exists 
		 (select 1 from biomart.bio_assay_data_annotation x
		  where fg.bio_assay_feature_group_id = x.bio_assay_feature_group_id
		    and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) = x.bio_marker_id);
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Link feature_group to bio_marker in biomart.bio_assay_data_annotation',SQL%ROWCOUNT,stepCt,'Done');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_rbm_annotation',0,stepCt,'Done');
	
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
 
