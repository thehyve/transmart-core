--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_PROTEOMICS_ANNOT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_PROTEOMICS_ANNOT" 
(
currentJobID NUMBER := null
 )
AS
/*************************************************************************
*This stored procedure is for ETL to load proteomics ANNOTATION 
* Date:10/29/2013
******************************************************************/

	--Audit variables
	newJobFlag INTEGER(1);
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID number(18,0); 
	stepCt number(18,0); 
	gplId	varchar2(100);

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
	cz_write_audit(jobId,databaseName,procedureName,'Starting I2B2_LOAD_PROTEOMICS_ANNOTTATION',0,stepCt,'Done');

	--	get  id_ref  from external table
	
      select distinct gpl_id into gplId from lt_protein_annotation ;

	
        --	delete any existing data from deapp.de_protien_annotation
        delete from deapp.de_protein_annotation
	where gpl_id =gplId;
        

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_protein_annotation',SQL%ROWCOUNT,stepCt,'Done');
	 	
	insert into  deapp.de_protein_annotation
	(gpl_id
	,peptide 
	,uniprot_id
	,biomarker_id
	,organism)
	select distinct d.gpl_id
	,trim(d.peptide)
	,d.uniprot_id --d.mirna_symbol
	,p.bio_marker_id
	,coalesce(d.organism,'Homo sapiens')
	from lt_protein_annotation d
	,biomart.bio_marker p
	where d.gpl_id = gplId
        and p.primary_external_id = d.uniprot_id 
	  ;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_protien_annotation',SQL%ROWCOUNT,stepCt,'Done');
		
	--	update gene_symbol if null
	
	update deapp.de_protein_annotation t
	set biomarker_id=(select min(b.bio_marker_name) as biomarker_id
				 from biomart.mirna_bio_marker b
				 where to_char(t.uniprot_id) = b.primary_external_id
				   and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'PROTEIN')
	where t.gpl_id = gplId
	  and t.biomarker_id is null
	  and t.uniprot_id is not null
	  and exists
		 (select 1 from biomart.mirna_bio_marker x
		  where to_char(t.uniprot_id) = x.primary_external_id
			and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'PROTEIN');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing uniprot_id in de_protien_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
        update DEAPP.DE_PROTEIN_ANNOTATION set uniprot_name = (select bio_marker_name
        from BIOMART.BIO_MARKER
        WHERE biomart.bio_marker.primary_external_id = deapp.de_protein_annotation.uniprot_id)
        where gpl_id = gplId;  
        
        commit;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update uniprot_name in DEAPP de_protein_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
        
	--	insert probesets into biomart.bio_assay_feature_group
	
	insert into biomart.mirna_bio_assay_feature_group
	(feature_group_name
	,feature_group_type)
	select distinct t.peptide, 'PEPTIDE' --ask
	from tm_lz.lt_protein_annotation t
	where not exists
		 (select 1 from biomart.mirna_bio_assay_feature_group x
		  where t.gpl_id = x.feature_group_name);
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert peptides into biomart.mirna_bio_assay_feature_group',SQL%ROWCOUNT,stepCt,'Done');
		  
	--	insert probesets into biomart.mirna_bio_assay_data_annotation
	
	insert into biomart.mirna_bio_assay_data_annot
	(bio_assay_feature_group_id
	,bio_marker_id)
	select distinct fg.bio_assay_feature_group_id
		  ,coalesce(bgs.bio_marker_id,bgi.bio_marker_id)
	from lt_protein_annotation t
		,biomart.mirna_bio_assay_feature_group fg
		,biomart.mirna_bio_marker bgs
		,biomart.mirna_bio_marker bgi
	where ( t.biomarker_id is not null or 
               t.uniprot_id is not null)
	  and t.peptide = fg.feature_group_name
	  and t.biomarker_id = bgs.bio_marker_name(+)
	  and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgs.organism)
	  and to_char(t.uniprot_id) = bgi.primary_external_id(+)
	  and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgi.organism)
	  and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) > 0
	  and not exists
		 (select 1 from biomart.mirna_bio_assay_data_annot x
		  where fg.bio_assay_feature_group_id = x.bio_assay_feature_group_id
		    and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) = x.bio_marker_id);
			
	stepCt := stepCt + 1; 
	cz_write_audit(jobId,databaseName,procedureName,'Link feature_group to bio_marker in biomart.mirna_bio_assay_data_annotation',SQL%ROWCOUNT,stepCt,'Done');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_proteomics_annottation',0,stepCt,'Done');
	
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
 
