--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_RNA_SEQ_ANNOTATION
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_RNA_SEQ_ANNOTATION" 
( rtn_code OUT NUMBER
) AS
gpl_rtn NUMBER;
missing_platform	exception;
BEGIN
 -- insert into "DEAPP"."DE_RNASEQ_ANNOTATION"
 
 select count(platform) into gpl_rtn from de_gpl_info where marker_type='RNASEQ' and platform is not null;
 if gpl_rtn=0 then
 raise missing_platform;
 end if;
 
 insert into DE_RNASEQ_ANNOTATION 
 (
   GPL_ID
  ,TRANSCRIPT_ID 
  ,GENE_SYMBOL
  ,GENE_ID
  ,ORGANISM
  ,PROBESET_ID
    )
    select g.platform
          ,a.transcript_id
          ,a.gene_symbol
          ,b.bio_marker_id
          ,a.organism
          ,pd.probeset_id
          from LT_RNASEQ_ANNOTATION a
               ,(select platform from de_gpl_info where marker_type='RNASEQ') g
               ,bio_marker b
               ,probeset_deapp pd
                where b.bio_marker_name=a.gene_symbol
                and a.transcript_id =pd.probeset;
               select 0 into rtn_code from dual;
               
               --update DE_RNASEQ_ANNOTATION 
          --     update DE_RNASEQ_ANNOTATION  set gene_id=
         --      ---Exceptions occur
               --
               EXCEPTION
               when missing_platform then
		--cz_write_audit(jobId,databasename,procedurename,'Platform data missing from one or more subject_sample mapping records',1,stepCt,'ERROR');
		--cz_error_handler(jobid,procedurename);
		--cz_end_audit (jobId,'FAIL');
		select 161 into rtn_code from dual;
                WHEN OTHERS THEN
		--Handle errors.
		--cz_error_handler (jobID, procedureName);
		--End Proc
		--cz_end_audit (jobID, 'FAIL');
		select 162  into rtn_code from dual;
 
END I2B2_RNA_SEQ_ANNOTATION;
/
 
