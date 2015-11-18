--
-- Name: i2b2_load_proteomics_annot(numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_load_proteomics_annot(currentjobid numeric DEFAULT NULL::numeric) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*************************************************************************
*This stored procedure is for ETL to load proteomics ANNOTATION 
* Date:10/29/2013
******************************************************************/
Declare
	--Audit variables
	newJobFlag NUMERIC(1);
	databaseName character varying(100);
	procedureName character varying(100);
	jobID numeric(18,0); 
	stepCt numeric(18,0); 
	gplId	character varying(100);
	rtnCd integer;
	errorNumber character varying;
	errorMessage character varying;
	rowCt integer;

BEGIN

	stepCt := 0; 

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_PROTEOMICS_ANNOT';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select cz_start_audit (procedureName, databaseName) into jobID;
	END IF;

	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Starting I2B2_LOAD_PROTEOMICS_ANNOTATION',0,stepCt,'Done') into rtnCd;

	--	get  id_ref  from external table
	
      select distinct gpl_id into gplId from lt_protein_annotation ;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	select cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_protein_annotation',rowCt,stepCt,'Done') into rtnCd;
        --	delete any existing data from deapp.de_protein_annotation
        begin
		delete from deapp.de_subject_protein_data where protein_annotation_id in (select id from deapp.de_protein_annotation where gpl_id = gplId);
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
        begin
		delete from deapp.de_protein_annotation
		where gpl_id =gplId;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	select cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_protein_annotation',rowCt,stepCt,'Done') into rtnCd;
	begin
	insert into  deapp.de_protein_annotation
	(gpl_id
	,peptide 
	,uniprot_id
	,biomarker_id
	,organism)
	select distinct d.gpl_id
	,trim(d.peptide)
	,d.uniprot_id
	,p.bio_marker_id
	,coalesce(d.organism,'Homo sapiens')
	from lt_protein_annotation d
	,biomart.bio_marker p
	where d.gpl_id = gplId
        and p.primary_external_id = d.uniprot_id 
	--  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens')
	 -- and (d.gpl_id is not null or d.gene_symbol is not null)
	  ;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	select cz_write_audit(jobId,databaseName,procedureName,'Updated missing uniprot_id in de_protein_annotation',rowCt,stepCt,'Done') into rtnCd;

	begin
        update DEAPP.DE_PROTEIN_ANNOTATION set uniprot_name = (select bio_marker_name
        from BIOMART.BIO_MARKER
        WHERE biomart.bio_marker.primary_external_id = deapp.de_protein_annotation.uniprot_id)
        where gpl_id = gplId;
        exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;  
        
	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
	select cz_write_audit(jobId,databaseName,procedureName,'Update uniprot_name in DEAPP de_protein_annotation',rowCt,stepCt,'Done') into rtnCd;
	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_proteomics_annotation',0,stepCt,'Done') into rtnCd;
	
       ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    select cz_end_audit (jobID, 'SUCCESS') into rtnCd; 
  END IF; 

  return rtnCd;
  
  EXCEPTION 
  WHEN OTHERS THEN
    errorNumber := SQLSTATE;
    errorMessage := SQLERRM;
    select cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
    select cz_end_audit (jobID, 'FAIL') into rtnCd;

  return rtnCd;
END;
$$;

