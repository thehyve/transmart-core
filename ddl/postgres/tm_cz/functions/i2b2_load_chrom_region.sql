--
-- Name: i2b2_load_chrom_region(numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--

CREATE OR REPLACE FUNCTION i2b2_load_chrom_region(currentjobid numeric DEFAULT (-1))
  RETURNS numeric 
  LANGUAGE plpgsql
  AS $BODY$

Declare
	--Audit variables
	newJobFlag		integer;
	databaseName 		VARCHAR(100);
	procedureName 		VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage		character varying;
	rtnCd			integer;

	gplId			character varying;
	organismId		character varying;
	sqlText			varchar(1000);

BEGIN
	stepCt := 0;

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_CHROM_REGION';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a sinde_gle procedure run and we need to create it
	--If Job ID does not exist, then this is a single procedure run and we need to create it

	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobID;
	END IF;

	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_chrom_region',0,stepCt,'Done') into rtnCd;

-- The data should already be in the landing zone (tm_lz.lt_chromosomal_region)


-- We now do some basic check's:
	-- + is chromosomal_region already in deapp.de_chromosomal_region (gpl_id/region_name)
	--   if true then remove these lines?
	-- + ...


-- insert region definitions into deapp-schema

	-- First remove previous definitions for gpl_id
	select distinct gpl_id INTO gplId FROM tm_lz.lt_chromosomal_region;

	begin
	delete from deapp.de_chromosomal_region
	where gpl_id = gplId;
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from deapp.de_chromosomal_region',rowCt,stepCt,'Done') into rtnCd;

	begin
	delete from deapp.de_gpl_info
	where platform = gplId;
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from deapp.de_gpl_info',rowCt,stepCt,'Done') into rtnCd;


        -- Insert platform into deapp.de_gpl_info
	select distinct organism INTO organismId FROM tm_lz.lt_chromosomal_region;
	begin
	insert into deapp.de_gpl_info 
	(
		platform
		, title
		, organism
		, annotation_date
		, marker_type
	)
	values (gplId, 'Agilent Probe', organismId, current_timestamp, 'Chromosomal');
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load platform info into deapp.de_gpl_info',rowCt,stepCt,'Done') into rtnCd;


	-- Next insert the new definitions
	BEGIN	
	insert into deapp.de_chromosomal_region 
  	(     GPL_ID
	    , REGION_NAME
	    , CHROMOSOME
	    , START_BP
	    , END_BP
	    , NUM_PROBES
	    , CYTOBAND
	    , GENE_SYMBOL
	    , GENE_ID
	    , ORGANISM
	)
	select    lz.GPL_ID
	        , lz.REGION_NAME
	    	, lz.CHROMOSOME
	    	, lz.START_BP
	    	, lz.END_BP
	    	, lz.NUM_PROBES
	    	, lz.CYTOBAND
	    	, lz.GENE_SYMBOL
	    	, lz.GENE_ID
	    	, lz.ORGANISM
	from tm_lz.lt_chromosomal_region lz;
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load chromosomal region data into deapp.de_chromosomal_region',rowCt,stepCt,'Done') into rtnCd;


-- wrapping up
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_chrom_region',0,stepCt,'Done') into rtnCd;

       ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;

	return 1;

END;

$BODY$;
