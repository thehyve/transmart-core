--
-- Name: i2b2_rna_seq_annotation(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION i2b2_rna_seq_annotation ( rtn_code OUT bigint
)  RETURNS bigint AS $body$
DECLARE

gpl_rtn bigint;
missing_platform	exception;

BEGIN
 -- insert into "DEAPP"."DE_RNASEQ_ANNOTATION"
 
 select count(platform) into gpl_rtn from de_gpl_info where marker_type='RNASEQ' and (platform IS NOT NULL AND platform::text <> '');
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
    PERFORM g.platform
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
               PERFORM 0 into rtn_code ;
               
               --update DE_RNASEQ_ANNOTATION 
          --     update DE_RNASEQ_ANNOTATION  set gene_id=
         --      ---Exceptions occur
               --
               EXCEPTION
               when missing_platform then
		--cz_write_audit(jobId,databasename,procedurename,'Platform data missing from one or more subject_sample mapping records',1,stepCt,'ERROR');
		--cz_error_handler(jobid,procedurename);
		--cz_end_audit (jobId,'FAIL');
		PERFORM 161 into rtn_code ;
                WHEN OTHERS THEN
		--Handle errors.
		--cz_error_handler (jobID, procedureName);
		--End Proc
		--cz_end_audit (jobID, 'FAIL');
		PERFORM 162  into rtn_code ;
 
END I2B2_RNA_SEQ_ANNOTATION;
 
$body$
LANGUAGE PLPGSQL;
