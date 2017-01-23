--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_EQTL_TOP50
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_EQTL_TOP50" 
(i_bio_assay_analysis_id number
,i_job_id        number := null
)
 AS
        --Audit variables
    newJobFlag     INTEGER(1);
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         number(18,0);
    stepCt         number(18,0);

        v_sqlText                varchar2(2000);

BEGIN
    --Set Audit Parameters
    newJobFlag := 0; -- False (Default)
    jobID := i_job_id;

    SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
    procedureName := $$PLSQL_UNIT;

    --Audit JOB Initialization
    --If Job ID does not exist, then this is a single procedure run and we need to create it
    IF(jobID IS NULL or jobID < 1)
    THEN
        newJobFlag := 1; -- True
        cz_start_audit (procedureName, databaseName, jobID);
    END IF;

    stepCt := 1;
    cz_write_audit(jobID,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done');

        --        delete existing data from bio_asy_analysis_eqtl_top50

        delete from biomart.bio_asy_analysis_eqtl_top50
        where bio_assay_analysis_id = i_bio_assay_analysis_id;
    stepCt := stepCt + 1;
        cz_write_audit(jobID,databaseName,procedureName,'Delete data for analysis from biomart.bio_asy_analysis_eqtls_top50',SQL%ROWCOUNT,stepCt,'Done');
        commit;

/*
        --        disable indexes

        for eqtl_idx in (select index_name
                                                        ,table_name
                                         from all_indexes
                                         where owner = 'BIOMART'
                                           and table_name = 'BIO_ASY_ANALYSIS_EQTL_TOP50')
        loop
                v_sqlText := 'alter index ' || eqtl_idx.index_name || ' unusable';
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Disabling index ' || eqtl_idx.index_name || ' on ' || eqtl_idx.table_name,0,stepCt,'Done');
                execute immediate(v_sqlText);
                stepCt := stepCt + 1;
                cz_write_audit(jobId,databaseName,procedureName,'Disabling complete',0,stepCt,'Done');
        end loop;
*/

        --        insert analysis into bio_asy_analysis_eqtl_top50

        insert into biomart.bio_asy_analysis_eqtl_top50
        (bio_assay_analysis_id
        ,analysis
        ,chrom
        ,pos
        ,rsgene
        ,rsid
        ,pvalue
        ,logpvalue
        ,extdata
        ,rnum
        ,intronexon
        ,recombinationrate
        ,regulome
        )
        select a.bio_assay_analysis_id
                  ,a.analysis
                  ,info.chrom
                  ,info.pos
                  ,a.gene
                  ,a.rsid
                  ,a.pvalue
                  ,a.logpvalue
                  ,a.extdata
                  ,a.rnum
                  ,info.exon_intron as intronexon
                  ,info.recombination_rate as recombinationrate
                  ,info.regulome_score as regulome
        from (select b.bio_assay_analysis_id
                                ,b.analysis
                                ,b.rsid
                                ,b.pvalue
                                ,b.logpvalue
                                ,b.extdata
                                ,b.gene
                                ,b.cis_trans
                                ,b.distance_from_gene
                                ,b.rnum
                 from (select eqtl.bio_assay_analysis_id
                                         ,baa.analysis_name as analysis
                                         ,eqtl.rs_id as rsid
                                         ,eqtl.p_value as pvalue
                                         ,eqtl.log_p_value as logpvalue
                                         ,eqtl.ext_data as extdata
                                         ,eqtl.gene
                                         ,eqtl.cis_trans
                                         ,eqtl.distance_from_gene
                                         ,row_number () over (order by eqtl.p_value asc, eqtl.rs_id asc) as rnum
                          from biomart.bio_assay_analysis_eqtl eqtl
                          inner join biomart.bio_assay_analysis baa
                                        on  baa.bio_assay_analysis_id = eqtl.bio_assay_analysis_id
                          where eqtl.bio_assay_analysis_id = i_bio_assay_analysis_id) b
                 where b.rnum < 500) a
        inner join deapp.de_rc_snp_info info
                  on  a.rsid = info.rs_id
                  and hg_version='19';
    stepCt := stepCt + 1;
        cz_write_audit(jobID,databaseName,procedureName,'Insert data for analysis from biomart.bio_asy_analysis_eaqtl_top50',SQL%ROWCOUNT,stepCt,'Done');
        commit;

/*
        --        rebuild indexes

        for eqtl_idx in (select index_name
                                                           ,table_name
                                                 from all_indexes
                                                 where owner = 'BIOMART'
                                                   and table_name = 'BIO_ASY_ANALYSIS_EQTL_TOP50')
                loop
                        v_sqlText := 'alter index ' || eqtl_idx.index_name || ' rebuild';
                        stepCt := stepCt + 1;
                        cz_write_audit(jobId,databaseName,procedureName,'Rebuilding index ' || eqtl_idx.index_name || ' on ' || eqtl_idx.table_name,0,stepCt,'Done');
                        execute immediate(v_sqlText);
                        stepCt := stepCt + 1;
                        cz_write_audit(jobId,databaseName,procedureName,'Rebuilding complete',SQL%ROWCOUNT,stepCt,'Done');
                end loop;
*/

        cz_write_audit(jobID,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done');
    stepCt := stepCt + 1;

    cz_end_audit(jobID, 'Success');

    exception
    when others then
    --Handle errors.
        cz_error_handler (jobID, procedureName);
    --End Proc
        cz_end_audit (jobID, 'FAIL');

END I2B2_LOAD_eqtl_TOP50;
/
