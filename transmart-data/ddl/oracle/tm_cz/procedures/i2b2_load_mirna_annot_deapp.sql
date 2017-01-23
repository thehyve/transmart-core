--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_MIRNA_ANNOT_DEAPP
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_MIRNA_ANNOT_DEAPP" 
(
currentJobID NUMBER := null
 )
AS
/*************************************************************************
*This stored procedure is for ETL to load QPCR MIRNA ANNOTATION 
* Date:10/29/2013
******************************************************************/

	--Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0); 
	stepCt number(18,0); 
	idREF	varchar2(100);

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
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_MIRNA_annot_deapp',0,stepCt,'Done');

	--	get  id_ref  from external table
	
	--select distinct id_ref into idREF from lt_qpcr_mirna_annotation where rownum=1;

	
/*	
	--	delete any existing data from probeset_deapp
	
	delete from probeset_deapp
	where platform = idREF;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from REFERENCE mirna_probeset_deapp',SQL%ROWCOUNT,stepCt,'Done');
*/
		
	--	delete any existing data from mirna_annotation_deapp
	
	delete from mirna_annotation_deapp
	where id_ref in ( select distinct id_ref from lt_qpcr_mirna_annotation)
  and gpl_id in ( select distinct gpl_id from lt_qpcr_mirna_annotation);

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from mirna_annotation_deapp',SQL%ROWCOUNT,stepCt,'Done');
        
        delete from deapp.de_qpcr_mirna_annotation
	where id_ref in (select distinct id_ref from lt_qpcr_mirna_annotation)
  and gpl_id in(select gpl_id from lt_qpcr_mirna_annotation);

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_qpcr_mirna_annotation',SQL%ROWCOUNT,stepCt,'Done');
        
        
      /*  delete from mirna_probeset_deapp
	where probeset in ( select distinct id_ref from lt_qpcr_mirna_annotation);

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from mirna_probeset_deapp',SQL%ROWCOUNT,stepCt,'Done');
        
        
        
	--	delete any existing data from deapp.de_qpcr_mirna_annotation

	delete from deapp.de_qpcr_mirna_annotation
	where id_ref in (select distinct id_ref from lt_qpcr_mirna_annotation);
   
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_qpcr_mirna_annotation',SQL%ROWCOUNT,stepCt,'Done');
      */
	
	--	insert any new probesets into mirna_probeset_deapp
	
	insert into mirna_probeset_deapp
	(probeset
	,organism
	,platform)
	select distinct id_ref
		  ,coalesce(organism,'Homo sapiens')
	      ,gpl_id
	from lt_qpcr_mirna_annotation t
	where not exists
		 (select 1 from mirna_probeset_deapp x
		  where 
                         t.id_ref = x.probeset
                         and t.gpl_id = x.platform
			--and coalesce(t.organism,'Homo sapiens') = coalesce(x.organism,'Homo sapiens')
                        )
	;
        commit;
      
	--where id_ref is not null 
	--   or mirna_symbol is not null;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert new probesets into mirna_probeset_deapp',SQL%ROWCOUNT,stepCt,'Done');
		
                --	update organism for existing probesets in mirna_probeset_deapp
	
	update mirna_probeset_deapp p
	set organism=(select distinct t.organism from lt_qpcr_mirna_annotation t
				  where p.probeset = t.id_ref
				    --and p.probeset = t.probe_id
                                    )
	where exists
		 (select 1 from lt_qpcr_mirna_annotation x
		  where p.platform = x.gpl_id
		    and p.probeset = x.id_ref);
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update organism in mirna_probeset_deapp',SQL%ROWCOUNT,stepCt,'Done');
			 
                
                
	--	insert data into mirna_annotation_deapp
	
	insert into mirna_annotation_deapp
	(id_ref
	,probe_id
	,mirna_symbol
	,mirna_id
	,probeset_id
	,organism
  ,gpl_id)
	select distinct d.id_ref
	,null
	,null
	,d.mirna_id
	,p.probeset_id
	,coalesce(d.organism,'Homo sapiens')
  ,d.gpl_id
	from lt_qpcr_mirna_annotation d
	,mirna_probeset_deapp p
	where d.id_ref = p.probeset
    and p.platform = d.gpl_id
	  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens')
	  --and (d.id_ref is not null or d.mirna_symbol is not null)
	  ;
	commit;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into REFERENCE mirna_annotation_deapp',SQL%ROWCOUNT,stepCt,'Done');
		
	--	insert data into deapp.de_qpcr_mirna_annotation
	
	insert into 
        deapp.de_qpcr_mirna_annotation
	(id_ref
	,probe_id
	,mirna_symbol
	,mirna_id
	,probeset_id
	,organism
  ,gpl_id)
	select distinct d.id_ref
	,null
	,null --d.mirna_symbol
	,lower(d.mirna_id) as mirna_id
	,p.probeset_id
	,coalesce(d.organism,'Homo sapiens')
  ,d.gpl_id
	from lt_qpcr_mirna_annotation d
	,mirna_probeset_deapp p
	where d.id_ref = p.probeset
	  and p.platform = d.gpl_id
	  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens');
	  --and d.id_ref not in (select distinct id_ref from deapp.de_qpcr_mirna_annotation )
	  --and (d.id_ref is not null or d.mirna_symbol is not null)
	  
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_qpcr_mirna_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
		/*
	--	update id_ref if null
	
	update deapp.de_qpcr_mirna_annotation t
	set id_ref=(select to_number(min(b.primary_external_id)) as mirna_id
				 from biomart.mirna_bio_marker b
				 where t.mirna_symbol = b.bio_marker_name
				   and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'MIRNA')
	where t.id_ref = idREF
	  and t.mirna_id is null 
	  and t.mirna_symbol is not null
	  and exists
		 (select 1 from biomart.mirna_bio_marker x
		  where t.mirna_symbol = x.bio_marker_name
			and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'MIRNA');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing id_ref in de_qpcr_mirna_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
	--	update gene_symbol if null
	
	update deapp.de_qpcr_mirna_annotation t
	set mirna_symbol=(select min(b.bio_marker_name) as mirna_symbol
				 from biomart.mirna_bio_marker b
				 where to_char(t.mirna_id) = b.primary_external_id
				   and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'MIRNA')
	where t.id_ref = idREF
	  and t.mirna_symbol is null
	  and t.mirna_id is not null
	  and exists
		 (select 1 from biomart.mirna_bio_marker x
		  where to_char(t.mirna_id) = x.primary_external_id
			and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'MIRNA');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing gene_id in de_qpcr_mirna_annotation',SQL%ROWCOUNT,stepCt,'Done');
	*/
	--	insert probesets into biomart.bio_assay_feature_group
	
	insert into biomart.mirna_bio_assay_feature_group
	(feature_group_name
	,feature_group_type)
	select distinct t.id_ref, 'PROBESET'
	from tm_lz.lt_qpcr_mirna_annotation t
	where not exists
		 (select 1 from biomart.mirna_bio_assay_feature_group x
		  where t.id_ref = x.feature_group_name);
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert probesets into biomart.mirna_bio_assay_feature_group',SQL%ROWCOUNT,stepCt,'Done');
		  
	--	insert probesets into biomart.mirna_bio_assay_data_annotation
	
	insert into biomart.mirna_bio_assay_data_annot
	(bio_assay_feature_group_id
	,bio_marker_id)
	select distinct fg.bio_assay_feature_group_id
		  ,coalesce(bgs.bio_marker_id,bgi.bio_marker_id)
	from lt_qpcr_mirna_annotation t
		,biomart.mirna_bio_assay_feature_group fg
		,biomart.mirna_bio_marker bgs
		,biomart.mirna_bio_marker bgi
	where ( --t.mirna_symbol is not null or 
               t.mirna_id is not null)
	  and t.id_ref = fg.feature_group_name
	  --and t.mirna_symbol = bgs.bio_marker_name(+)
	  and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgs.organism)
	  and to_char(t.mirna_id) = bgi.primary_external_id(+)
	  and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgi.organism)
	  and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) > 0
	  and not exists
		 (select 1 from biomart.mirna_bio_assay_data_annot x
		  where fg.bio_assay_feature_group_id = x.bio_assay_feature_group_id
		    and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) = x.bio_marker_id);
			 
	stepCt := stepCt + 1; 
	cz_write_audit(jobId,databaseName,procedureName,'Link feature_group to bio_marker in biomart.mirna_bio_assay_data_annotation',SQL%ROWCOUNT,stepCt,'Done');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_MIRNA_annot_deapp',0,stepCt,'Done');
	
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
 
