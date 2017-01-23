create or replace PROCEDURE I2B2_LOAD_EQTL_TOP50
(i_bio_assay_analysis_id number
,i_job_id	number := null
)
 AS 
	--Audit variables
    newJobFlag     INTEGER(1);
    databaseName     VARCHAR(100);
    procedureName VARCHAR(100);
    jobID         number(18,0);
    stepCt         number(18,0);
	
	v_sqlText		varchar2(2000);
	
BEGIN
    --Set Audit Parameters
    newJobFlag := 0; -- False (Default)
    jobID := -1;

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
    cz_write_audit(jobId,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done');
    
	--	delete existing data from bio_asy_analysis_eqtl_top50
	
	delete from biomart.bio_asy_analysis_eqtl_top50
	where bio_assay_analysis_id = i_bio_assay_analysis_id;
    stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete data for analysis from biomart.bio_asy_analysis_eqtls_top50',SQL%ROWCOUNT,stepCt,'Done');
	commit; 
	
/*
	--	disable indexes 

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
	
	--	insert analysis into bio_asy_analysis_eqtl_top50
	
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
	cz_write_audit(jobId,databaseName,procedureName,'Insert data for analysis from biomart.bio_asy_analysis_eaqtl_top50',SQL%ROWCOUNT,stepCt,'Done');
	commit; 

/*
	--	rebuild indexes
	
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

	cz_write_audit(jobId,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done');
    stepCt := stepCt + 1;
    
    cz_end_audit(jobId, 'Success');
    
    exception
    when others then
    --Handle errors.
        cz_error_handler (jobID, procedureName);
    --End Proc
        cz_end_audit (jobID, 'FAIL');
		
END I2B2_LOAD_eqtl_TOP50;	

/*
execute immediate ('drop table biomart.tmp_analysis_count_eqtl');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate ('create table biomart.tmp_analysis_count_eqtl as
select count(*) as total, bio_assay_analysis_id
from biomart.bio_assay_analysis_eqtl
group by bio_assay_analysis_id');


execute immediate ('update biomart.bio_assay_analysis b
set b.data_count = (select a.total from biomart.tmp_analysis_count_eqtl  a where a.bio_assay_analysis_id =  b.bio_assay_analysis_id)
where exists(
select 1 from biomart.tmp_analysis_count_eqtl  a where a.bio_assay_analysis_id =  b.bio_assay_analysis_id
)');

--select * from bio_assay_analysis_eqtl 
--where bio_assay_analysis_id = 419842521
--order by p_value asc, rs_id asc;

--select * from tmp_analysis_eqtl_top500
--where bio_assay_analysis_id = 419842521
--order by p_value asc;

BEGIN
execute immediate ('drop table biomart.tmp_analysis_eqtl_top500');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate ('create table biomart.tmp_analysis_eqtl_top500 
as
select a.* 
from (
select 
bio_asy_analysis_eqtl_id,
bio_assay_analysis_id,
rs_id,
p_value,
log_p_value,
etl_id,
ext_data,
p_value_char,
gene,
cis_trans,
distance_from_gene,
row_number () over (partition by bio_assay_analysis_id order by p_value asc, rs_id asc) as rnum
from biomart.bio_assay_analysis_eqtl
) a
where 
a.rnum <=500');

execute immediate ('create index BIOMART.t_a_ge_t500_idx on BIOMART.TMP_ANALYSIS_eqtl_TOP500(RS_ID) tablespace "INDX"');
execute immediate ('create index BIOMART.t_a_gae_t500_idx on BIOMART.TMP_ANALYSIS_eqtl_TOP500(bio_assay_analysis_id) tablespace "INDX"');

BEGIN
execute immediate ('drop table biomart.bio_asy_analysis_eqtl_top50 cascade constraints');
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;

execute immediate ('create table biomart.BIO_ASY_ANALYSIS_eqtl_TOP50
as 
SELECT baa.bio_assay_analysis_id,
baa.analysis_name AS analysis, info.chrom AS chrom, info.pos AS pos,
gmap.gene_name AS rsgene, DATA.rs_id AS rsid,
DATA.p_value AS pvalue, DATA.log_p_value AS logpvalue, data.gene as gene,
DATA.ext_data AS extdata , DATA.rnum,
info.exon_intron as intronexon, info.recombination_rate as recombinationrate, info.regulome_score as regulome
FROM biomart.tmp_analysis_eqtl_top500 DATA 
JOIN biomart.bio_assay_analysis baa 
ON baa.bio_assay_analysis_id = DATA.bio_assay_analysis_id
JOIN deapp.de_rc_snp_info info ON DATA.rs_id = info.rs_id and (hg_version='''||19||''')
LEFT JOIN deapp.de_snp_gene_map gmap ON  gmap.snp_name =info.rs_id');

--execute immediate ('select count(*) from BIO_ASY_ANALYSIS_eqtl_TOP50');

execute immediate ('create index BIOMART.B_ASY_eqtl_T50_IDX1 on BIOMART.BIO_ASY_ANALYSIS_eqtl_TOP50(bio_assay_analysis_id) parallel tablespace "INDX"');

execute immediate ('create index BIOMART.B_ASY_eqtl_T50_IDX2 on BIOMART.BIO_ASY_ANALYSIS_eqtl_TOP50(ANALYSIS) parallel tablespace "INDX"');

END I2B2_LOAD_EQTL_TOP50;

*/