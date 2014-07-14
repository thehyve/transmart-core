--
-- Name: i2b2_load_mirna_annot_deapp(numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_load_mirna_annot_deapp(currentjobid numeric DEFAULT NULL::numeric) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*************************************************************************
*This stored procedure is for ETL to load QPCR MIRNA ANNOTATION 
* Date:10/29/2013
******************************************************************/
DECLARE
	--Audit variables
	newJobFlag numeric(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID numeric(18,0); 
	stepCt numeric(18,0); 
	idREF	varchar(100);
	rowCt integer;

BEGIN

	stepCt := 0; 

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_MIRNA_ANNOT_DEAPP';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select cz_start_audit (procedureName, databaseName) into jobID;
	END IF;

	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_MIRNA_annot_deapp',0,stepCt,'Done');

	begin
	delete from mirna_annotation_deapp
	where id_ref in ( select distinct id_ref from tm_lz.lt_qpcr_mirna_annotation)
  and gpl_id in ( select distinct gpl_id from tm_lz.lt_qpcr_mirna_annotation);
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from mirna_annotation_deapp',rowCt,stepCt,'Done');

        begin
        delete from deapp.de_qpcr_mirna_annotation
	where id_ref in (select distinct id_ref from tm_lz.lt_qpcr_mirna_annotation)
  and gpl_id in(select gpl_id from tm_lz.lt_qpcr_mirna_annotation);
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_qpcr_mirna_annotation',rowCt,stepCt,'Done');

	begin
	insert into mirna_probeset_deapp
	(probeset
	,organism
	,platform)
	select distinct id_ref
		  ,coalesce(organism,'Homo sapiens')
	      ,gpl_id
	from tm_lz.lt_qpcr_mirna_annotation t
	where not exists
		 (select 1 from mirna_probeset_deapp x
		  where 
                         t.id_ref = x.probeset
                         and t.gpl_id = x.platform
			--and coalesce(t.organism,'Homo sapiens') = coalesce(x.organism,'Homo sapiens')
                        )
	;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
        
      
	--where id_ref is not null 
	--   or mirna_symbol is not null;
	
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert new probesets into mirna_probeset_deapp',rowCt,stepCt,'Done');
		
                --	update organism for existing probesets in mirna_probeset_deapp

	begin
	update mirna_probeset_deapp p
	set organism=(select distinct t.organism from tm_lz.lt_qpcr_mirna_annotation t
				  where p.probeset = t.id_ref
				    --and p.probeset = t.probe_id
                                    )
	where exists
		 (select 1 from tm_lz.lt_qpcr_mirna_annotation x
		  where p.platform = x.gpl_id
		    and p.probeset = x.id_ref);
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Update organism in mirna_probeset_deapp',rowCt,stepCt,'Done');
			 
                
                
	--	insert data into mirna_annotation_deapp
	begin
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
	from tm_lz.lt_qpcr_mirna_annotation d
	,mirna_probeset_deapp p
	where d.id_ref = p.probeset
    and p.platform = d.gpl_id
	  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens')
	  --and (d.id_ref is not null or d.mirna_symbol is not null)
	  ;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into REFERENCE mirna_annotation_deapp',rowCt,stepCt,'Done');
		
	--	insert data into deapp.de_qpcr_mirna_annotation

	begin
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
	from tm_lz.lt_qpcr_mirna_annotation d
	,mirna_probeset_deapp p
	where d.id_ref = p.probeset
	  and p.platform = d.gpl_id
	  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens');	  
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_qpcr_mirna_annotation',rowCt,stepCt,'Done');
	
	insert into biomart.mirna_bio_assay_feature_group
	(feature_group_name
	,feature_group_type)
	select distinct t.id_ref, 'PROBESET'
	from tm_lz.lt_qpcr_mirna_annotation t
	where not exists
		 (select 1 from biomart.mirna_bio_assay_feature_group x
		  where t.id_ref = x.feature_group_name);
			
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert probesets into biomart.mirna_bio_assay_feature_group',rowCt,stepCt,'Done');
		  
	--	insert probesets into biomart.mirna_bio_assay_data_annotation
	begin
	insert into biomart.mirna_bio_assay_data_annot
	(bio_assay_feature_group_id
	,bio_marker_id)
	select distinct fg.bio_assay_feature_group_id
		  ,bgi.bio_marker_id
	from tm_lz.lt_qpcr_mirna_annotation t left outer join biomart.bio_marker bgi on bio_marker_type = 'MIRNA' and t.mirna_id::varchar = bgi.primary_external_id
		,biomart.mirna_bio_assay_feature_group fg
	where (
               t.mirna_id is not null)
	  and t.id_ref = fg.feature_group_name
	  and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgi.organism)
	  and coalesce(bgi.bio_marker_id,-1) > 0
	  and not exists
		 (select 1 from biomart.mirna_bio_assay_data_annot x
		  where fg.bio_assay_feature_group_id = x.bio_assay_feature_group_id
		    and coalesce(bgi.bio_marker_id,-1) = x.bio_marker_id);
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT; 
	perform cz_write_audit(jobId,databaseName,procedureName,'Link feature_group to bio_marker in biomart.mirna_bio_assay_data_annotation',rowCt,stepCt,'Done');
			
	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_MIRNA_annot_deapp',0,stepCt,'Done');
	
       ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    perform cz_end_audit (jobID, 'SUCCESS'); 
  END IF;

  return 1;
  EXCEPTION 
  WHEN OTHERS THEN
    --Handle errors.
    perform cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
    --End Proc
    perform cz_end_audit (jobID, 'FAIL');

    return -16;
END;
$$;

