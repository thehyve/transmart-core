-- Function: i2b2_rna_seq_annotation()

CREATE FUNCTION i2b2_rna_seq_annotation(currentjobid bigint DEFAULT NULL::bigint)
  RETURNS numeric AS
$BODY$
DECLARE

	gpl_rtn bigint;
	newJobFlag numeric(1);
	databaseName character varying(100);
	procedureName character varying(100);
	jobID bigint;
	errorNumber		character varying;
	errorMessage	character varying;
	rtnCd			integer;
	rowCt			numeric(18,0);
	stepCt bigint;
BEGIN

	stepCt := 0;

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;
	
	databaseName := 'TM_CZ';
	procedureName := 'I2B2_RNA_SEQ_ANNOTATION';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(coalesce(jobID::text, '') = '' or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select cz_start_audit (procedureName, databaseName, jobID) into jobId;
	END IF;

	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_rna_seq_annotation',0,stepCt,'Done') into rtnCd;
	
 -- insert into "DEAPP"."DE_RNASEQ_ANNOTATION"
 
 select count(platform) into gpl_rtn from deapp.de_gpl_info where marker_type='RNASEQ' and (platform IS NOT NULL AND platform::text <> '');
 if gpl_rtn=0 then
		select cz_write_audit(jobId,databasename,procedurename,'Platform data missing from DEAPP.DE_GPL_INFO',1,stepCt,'ERROR') into rtnCd;
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return 161;
 end if;
 
 begin
 insert into deapp.DE_RNASEQ_ANNOTATION
 (
 TRANSCRIPT_ID
   ,GPL_ID
    ,GENE_SYMBOL
  ,GENE_ID
  ,ORGANISM
    )
    select distinct (a.transcript_id)
      ,null
          ,a.gene_symbol
          ,null
          ,a.organism
          from tm_lz.LT_RNASEQ_ANNOTATION a
                where a.transcript_id not in (select distinct transcript_id from deapp.DE_RNASEQ_ANNOTATION);
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Insert data in DE_RNASEQ_ANNOTATION',0,stepCt,'Done') into rtnCd;
	
	begin
    update deapp.DE_RNASEQ_ANNOTATION a set GENE_ID=(select distinct primary_external_id from biomart.bio_marker b where
	b.bio_marker_name=a.gene_symbol limit 1)
    where a.GENE_ID is null;
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'End i2b2_rna_seq_annotation',0,stepCt,'Done') into rtnCd;
	
       ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    select cz_end_audit (jobID, 'SUCCESS') into rtnCd;
  END IF;

  return 0;
 
END;
 
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ALTER FUNCTION i2b2_rna_seq_annotation(bigint)
--  OWNER TO tm_cz;
-- GRANT EXECUTE ON FUNCTION i2b2_rna_seq_annotation(bigint) TO tm_cz;
-- REVOKE ALL ON FUNCTION i2b2_rna_seq_annotation(bigint) FROM public;
