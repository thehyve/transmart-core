--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_CHROM_REGION
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_CHROM_REGION" 
(
  platform_t in varchar2 := ''
, data_type in varchar2 := 'ACGH' -- valid values are ACGH and RNASEQ
, genome_release in varchar2 := ''
, currentjobid in number := NULL
) as
 --Audit variables
  newJobFlag     INTEGER;
  databaseName   VARCHAR(100);
  procedureName  VARCHAR(100);
  jobID          NUMBER(18,0);
  stepCt         NUMBER(18,0);
  gplId          VARCHAR2(100);
  marker_type    VARCHAR2(100);
  organismId     VARCHAR2(100);
  platform_title VARCHAR(500);

begin
    stepCt := 0;

  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID      := currentJobID;

  SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
  procedureName := $$PLSQL_UNIT;

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(jobID IS NULL OR jobID < 1) THEN
    newJobFlag := 1; -- True
    cz_start_audit (procedureName, databaseName, jobID);
  END IF;

  stepCt := stepCt + 1;
  tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_chrom_region',0,stepCt,'Done');


  -- The data should already be in the landing zone (tm_lz.lt_chromosomal_region)
  -- We now do some basic check's:
  -- + is chromosomal_region already in deapp.de_chromosomal_region (gpl_id/region_name)
  --   if true then remove these lines?
  -- + ...

  -- insert region definitions into deapp-schema
  -- First remove previous definitions for gpl_id
  SELECT DISTINCT gpl_id INTO gplId FROM lt_chromosomal_region;

  DELETE FROM deapp.de_chromosomal_region WHERE gpl_id = gplId;
  stepCt                := stepCt + 1;
  tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from deapp.de_chromosomal_region for plaform: ' || gplId,SQL%ROWCOUNT,stepCt,'Done');

  DELETE FROM deapp.de_gpl_info WHERE platform = gplId;
  stepCt                := stepCt + 1;
  tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from deapp.de_gpl_info for platform: ' || gplID,SQL%ROWCOUNT,stepCt,'Done');

  -- Insert platform into deapp.de_gpl_info

  -- Derive marker_type from data_type argument (defaults to CHROMOSOME_REGION_ACGH)
  if (upper(data_type) = 'RNASEQ')
  then
    marker_type := 'RNASEQ_RCNT';
  else
    marker_type := 'Chromosomal';
  end if;

  platform_title := platform_t;
  IF (LENGTH(platform_title) = 0) THEN
      platform_title := gplId;
  END IF;
  SELECT DISTINCT organism INTO organismId FROM tm_lz.lt_chromosomal_region;

  INSERT INTO deapp.de_gpl_info
          (
            platform ,
            title ,
            organism ,
            annotation_date ,
            marker_type ,
            release_nbr
          )
          VALUES
          (
            gplId,
            platform_title,
            organismId,
            CURRENT_TIMESTAMP,
            marker_type,
            genome_release
          );
  stepCt := stepCt + 1;
  tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load platform info into deapp.de_gpl_info for platform: ' || gplID,SQL%ROWCOUNT,stepCt,'Done');

  -- Next insert the new definitions
  INSERT INTO deapp.de_chromosomal_region
            (
              GPL_ID ,
              REGION_NAME ,
              CHROMOSOME ,
              START_BP ,
              END_BP ,
              NUM_PROBES ,
              CYTOBAND ,
              GENE_SYMBOL ,
              GENE_ID ,
              ORGANISM
            )
          SELECT lz.GPL_ID ,
            lz.REGION_NAME ,
            lz.CHROMOSOME ,
            lz.START_BP ,
            lz.END_BP ,
            lz.NUM_PROBES ,
            lz.CYTOBAND ,
            lz.GENE_SYMBOL ,
            lz.GENE_ID ,
            lz.ORGANISM
          FROM tm_lz.lt_chromosomal_region lz;
          stepCt                := stepCt + 1;
          tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load chromosomal region data into deapp.de_chromosomal_region for platform: ' || gplId,SQL%ROWCOUNT,stepCt,'Done');

  -- update gene_id if null
  update de_chromosomal_region t
	set gene_id=(select to_number(min(b.primary_external_id)) as gene_id
				 from biomart.bio_marker b
				 where t.gene_symbol = b.bio_marker_name
				   and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'GENE')
	where t.gpl_id = gplId
	  and t.gene_id is null
	  and t.gene_symbol is not null
	  and exists
		 (select 1 from biomart.bio_marker x
		  where t.gene_symbol = x.bio_marker_name
			and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'GENE');
  stepCt                := stepCt + 1;
  tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Updated missing gene_id in de_chromosomal_region',SQL%ROWCOUNT,stepCt,'Done');

  -- wrapping up
  stepCt := stepCt + 1;
  tm_cz.cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_chrom_region',0,stepCt,'Done');

  ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1 THEN
    tm_cz.cz_end_audit (jobID, 'SUCCESS');
  END IF;

  EXCEPTION
  WHEN OTHERS THEN
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');

end i2b2_load_chrom_region;
/
