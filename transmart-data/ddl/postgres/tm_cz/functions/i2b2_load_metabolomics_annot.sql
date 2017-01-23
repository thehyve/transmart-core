--
-- Name: i2b2_load_metabolomics_annot(numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--

SET search_path = tm_cz, pg_catalog;

CREATE OR REPLACE FUNCTION i2b2_load_metabolomics_annot(currentjobid numeric DEFAULT NULL::numeric) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*************************************************************************
*This stored procedure is for ETL to load METABOLOMICS ANNOTATION
* Date:12/29/2013
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
	gplId VARCHAR(100);

BEGIN
  	stepCt := 0; 

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_METABOLOMICS_ANNOTATION';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select cz_start_audit (procedureName, databaseName) into jobID;
	END IF;
	
    stepCt := stepCt + 1;
    perform cz_write_audit(jobId,databaseName,procedureName,'Starting I2B2_LOAD_METABOLOMICS_ANNOTATION',0,stepCt,'Done');

    --    get  id_ref  from external table
    
	select distinct gpl_id into gplId from tm_lz.lt_metabolomic_annotation;
      
	--    delete any existing data from de_metabolite_sub_pway_metab
	begin
	delete from deapp.de_metabolite_sub_pway_metab where not exists (select id from de_metabolite_sub_pathways where de_metabolite_sub_pathways.id = de_metabolite_sub_pway_metab.sub_pathway_id) ;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	begin
	delete from deapp.de_metabolite_sub_pway_metab where sub_pathway_id in (select id from  de_metabolite_sub_pathways where gpl_id = gplId) ;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;

	perform cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_sub_pway_metab',rowCt,stepCt,'Done');

	--    delete any existing data from de_metabolite_sub_pathways
	begin
	delete from de_metabolite_sub_pathways where gpl_id = gplId;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;

	perform cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_sub_pathways',rowCt,stepCt,'Done');

  --    delete any existing data from de_metabolite_super_pathways

	begin
    delete from deapp.de_metabolite_super_pathways
    where gpl_id = gplId;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;

	perform cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_super_pathways',rowCt,stepCt,'Done');

    --    delete any existing data from deapp.de_metabolite_annotation
	begin
        delete from deapp.de_metabolite_annotation
        where gpl_id = gplId;
       exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;

    perform cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_annotation',rowCt,stepCt,'Done');

    
	begin
    insert into  deapp.de_metabolite_annotation
        (
        id
        ,gpl_id
        ,biochemical_name
        ,biomarker_id
        ,hmdb_id
        )
    select
        nextval('deapp.metabolomics_annot_id')
        ,d.gpl_id
    ,trim(d.biochemical_name)
    ,b.primary_external_id
    ,d.hmdb_id
    from lt_metabolomic_annotation d
    left outer join bio_marker b on b.bio_marker_name = d.biochemical_name
    --,peptide_deapp p
    where d.gpl_id = gplId;
    exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;  
    
    perform cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_annotation',rowCt,stepCt,'Done');
        
	begin
     insert into deapp.de_metabolite_super_pathways
        (
        id
        ,gpl_id
        ,super_pathway_name
        )
    select
        nextval('deapp.metabolite_sup_pth_id')
        ,d.gpl_id
    ,d.super_pathway
    from (select distinct gpl_id,super_pathway from lt_metabolomic_annotation ) d
    where d.gpl_id = gplId;
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
     
    perform cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_super_pathways',rowCt,stepCt,'Done');
                           
		begin
        insert into  deapp.de_metabolite_sub_pathways
        (
        id
        ,gpl_id
        ,sub_pathway_name
        ,super_pathway_id
        )
        select
        nextval('deapp.metabolite_sub_pth_id')
        ,d.gpl_id
    ,trim(d.sub_pathway)
        ,sp.id
    from (select unnest(regexp_split_to_array(sub_pathway, ';')) AS sub_pathway ,gpl_id,super_pathway
        FROM tm_lz.lt_metabolomic_annotation) as d
    ,deapp.de_metabolite_super_pathways sp
    where
        trim(d.super_pathway) = trim(sp.super_pathway_name)
        and d.gpl_id = gplId
        and sp.gpl_id = gplId;     
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;		
    
    perform cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_sub_pathways',rowCt,stepCt,'Done');
                                                             
    begin
        insert into  deapp.de_metabolite_sub_pway_metab
        (
          metabolite_id
          ,sub_pathway_id
        )
	   select d.id, sp.id from deapp.de_metabolite_annotation d, deapp.de_metabolite_sub_pathways sp,
	(select unnest(regexp_split_to_array(sub_pathway, ';')) AS sub_pathway ,biochemical_name, gpl_id
			FROM tm_lz.lt_metabolomic_annotation) as lma
	where trim(lma.biochemical_name) = trim(d.biochemical_name)
	and trim(lma.sub_pathway) = trim (sp.sub_pathway_name)
	and d.gpl_id =gplId
			and lma.gpl_id=gplId;
		exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
            
    perform cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_sub_pway_metab',rowCt,stepCt,'Done');
        
    --    update biomarker_id if null
    
	begin
        update deapp.de_metabolite_annotation t
    set biomarker_id=(select min(b.bio_marker_name) as biomarker_id
                 from biomart.mirna_bio_marker b
                 where t.biomarker_id::text = b.primary_external_id
                   and upper(b.bio_marker_type) = 'metabolomic')
    where t.gpl_id = gplId
      and t.biomarker_id is null
      and t.biochemical_name is not null
      and exists
         (select 1 from biomart.mirna_bio_marker x
          where t.biomarker_id::text = x.primary_external_id
            and upper(x.bio_marker_type) = 'metabolomic');
    exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	get diagnostics rowCt := ROW_COUNT;
	
    perform cz_write_audit(jobId,databaseName,procedureName,'Updated missing uniprotid in de_protien_annotation',rowCt,stepCt,'Done');
    
    --    insert probesets into biomart.bio_assay_feature_group
    begin
    insert into biomart.mirna_bio_assay_feature_group
    (feature_group_name
    ,feature_group_type)
    select distinct t.biochemical_name, 'METABOLOMIC' --ask
    from tm_lz.lt_metabolomic_annotation t        
    where not exists
         (select 1 from biomart.mirna_bio_assay_feature_group x
          where t.gpl_id = x.feature_group_name);
	exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
            
    perform cz_write_audit(jobId,databaseName,procedureName,'Insert peptides into biomart.mirna_bio_assay_feature_group',rowCt,stepCt,'Done');
          
    --    insert probesets into biomart.mirna_bio_assay_data_annotation
    
	begin
    insert into biomart.mirna_bio_assay_data_annot
    (bio_assay_feature_group_id
    ,bio_marker_id)
    select distinct fg.bio_assay_feature_group_id
          ,coalesce(bgs.bio_marker_id,bgi.bio_marker_id)
    from lt_metabolomic_annotation t
        inner join biomart.mirna_bio_assay_feature_group fg on t.biochemical_name = fg.feature_group_name
        left outer join biomart.mirna_bio_marker bgs on t.biochemical_name = bgs.bio_marker_name
        left outer join biomart.mirna_bio_marker bgi on t.hmdb_id::text= bgi.primary_external_id
    where (t.hmdb_id is not null)
      and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) > 0
      and not exists
         (select 1 from biomart.mirna_bio_assay_data_annot x
          where fg.bio_assay_feature_group_id = x.bio_assay_feature_group_id
            and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) = x.bio_marker_id);
            exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
    perform cz_write_audit(jobId,databaseName,procedureName,'Link feature_group to bio_marker in biomart.mirna_bio_assay_data_annotation',rowCt,stepCt,'Done');
            
        -- Inserts subpathways into search_keyword,
-- subpathways into search_keyword_term,
-- superpathways into search_keyword,
-- superpathways into search_keyword_term
			begin
          INSERT INTO searchapp.search_keyword (
            keyword,
            bio_data_id,
            unique_id,
            data_category,
            display_data_category)
          SELECT
            CONCAT(CONCAT(subp.sub_pathway_name, '_'), subp.gpl_id),
            subp.id,
            CONCAT('METABOLITE_SUBPATHWAY:', subp.id),
            'METABOLITE_SUBPATHWAY',
            'Metabolite subpathway'
          FROM
            deapp.de_metabolite_sub_pathways subp
            where subp.gpl_id = gplId;
exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
    perform cz_write_audit(jobId,databaseName,procedureName,'Insert subpathways into search_keyword',rowCt,stepCt,'Done');
    
		begin
            INSERT INTO searchapp.search_keyword_term (
              keyword_term,
              search_keyword_id,
              rank,
              term_length)
            SELECT
              upper_keyword,
              search_keyword_id,
              1,
              LENGTH(upper_keyword)
            FROM (
              SELECT
                UPPER(skw.keyword) AS upper_keyword,
                skw.search_keyword_id AS search_keyword_id
              FROM
                searchapp.search_keyword skw
              WHERE
                data_category = 'METABOLITE_SUBPATHWAY'
              AND
                bio_data_id IN (
                  SELECT
                    subp.id
                  FROM
                    deapp.de_metabolite_sub_pathways subp
                    where subp.gpl_id = gplId
                )
            ) as s;

            stepCt := stepCt + 1;
            perform cz_write_audit(jobId,databaseName,procedureName,'Insert subpathways into search_keyword_term',rowCt,stepCt,'Done');
            
            INSERT INTO searchapp.search_keyword (
              keyword,
              bio_data_id,
              unique_id,
              data_category,
              display_data_category)
            SELECT
              CONCAT(CONCAT(supp.super_pathway_name, '_'), supp.gpl_id),
              supp.id,
              CONCAT('METABOLITE_SUPERPATHWAY:', supp.id),
              'METABOLITE_SUPERPATHWAY',
              'Metabolite superpathway'
            FROM
              deapp.de_metabolite_super_pathways supp
              where supp.gpl_id = gplId;
			  exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;

            perform cz_write_audit(jobId,databaseName,procedureName,'Insert superpathways into search_keyword',rowCt,stepCt,'Done');
			
			begin
            INSERT INTO searchapp.search_keyword_term (
              keyword_term,
              search_keyword_id,
              rank,
              term_length)
            SELECT
              upper_keyword,
              search_keyword_id,
              1,
              LENGTH(upper_keyword)
            FROM (
              SELECT
                UPPER(skw.keyword) AS upper_keyword,
                skw.search_keyword_id AS search_keyword_id
              FROM
                searchapp.search_keyword skw
              WHERE
                data_category = 'METABOLITE_SUPERPATHWAY'
              AND
                bio_data_id IN (
                  SELECT
                    supp.id
                  FROM
                    deapp.de_metabolite_super_pathways supp
                    where supp.gpl_id = gplId
                )
            ) as s;
			exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;

        stepCt := stepCt + 1;
        perform cz_write_audit(jobId,databaseName,procedureName,'Insert superpathways into search_keyword_term',rowCt,stepCt,'Done');
                       
	begin
    INSERT INTO biomart.bio_marker (
          bio_marker_name,
          bio_marker_description,
          primary_external_id,
          bio_marker_type)
        SELECT
          CONCAT('PRIVATE:', annotation.id),
          CONCAT('PRIVATE:', annotation.id),
          CONCAT('PRIVATE:', annotation.id),
          'METABOLITE'
        FROM
          deapp.de_metabolite_annotation annotation
        WHERE
          annotation.hmdb_id IS NULL;
         exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT; 
        perform cz_write_audit(jobId,databaseName,procedureName,'Insert into biomart.bio_marker',rowCt,stepCt,'Done');
                 
		begin
        UPDATE deapp.de_metabolite_annotation annotation
        SET  hmdb_id = CONCAT('PRIVATE:', annotation.id)
        WHERE  annotation.hmdb_id IS NULL;
		exception
	when others then
		perform tm_cz.cz_error_handler (jobID, procedureName, SQLSTATE, SQLERRM);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1;
	get diagnostics rowCt := ROW_COUNT;
                 
        perform cz_write_audit(jobId,databaseName,procedureName,'Update deapp.de_metabolite_annotation',rowCt,stepCt,'Done');                 
        
        stepCt := stepCt + 1;
    perform cz_write_audit(jobId,databaseName,procedureName,'End I2B2_LOAD_METABOLOMICS_ANNOT',0,stepCt,'Done');
        
       ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
   perform cz_end_audit (jobID, 'SUCCESS');
  END IF;

  return 1;

END;
$$;

