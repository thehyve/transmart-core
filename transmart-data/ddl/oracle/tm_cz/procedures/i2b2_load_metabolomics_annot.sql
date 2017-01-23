--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_LOAD_METABOLOMICS_ANNOT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_LOAD_METABOLOMICS_ANNOT" 
(
currentJobID NUMBER := null
 )
AS
/*************************************************************************
*This stored procedure is for ETL to load METABOLOMICS ANNOTATION 
* Date:12/29/2013
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
	cz_write_audit(jobId,databaseName,procedureName,'Starting I2B2_LOAD_METABOLOMICS_ANNOTTATION',0,stepCt,'Done');

	--	get  id_ref  from external table
	
        select distinct gpl_id into gplId from tm_lz.lt_metabolomic_annotation;
        
        --	delete any existing data from de_metabolite_sub_pway_metab
        delete from deapp.de_metabolite_sub_pway_metab where sub_pathway_id in (select id from de_metabolite_sub_pathways where gpl_id = gplId) ;
        
      --	delete any existing data from de_metabolite_sub_pathways
        delete from de_metabolite_sub_pathways where gpl_id = gplId;

        stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_sub_pathways',SQL%ROWCOUNT,stepCt,'Done');
    
      	--	delete any existing data from de_metabolite_super_pathways
	
	delete from deapp.de_metabolite_super_pathways
	where gpl_id = gplId;
        
        stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_super_pathways',SQL%ROWCOUNT,stepCt,'Done');

  --quick fix for duplicated rows in keyword_term, as annotation is deleted and added again instead of being uploaded
  delete from searchapp.search_keyword_term where search_keyword_id in (select search_keyword_id from searchapp.search_keyword sk where data_category='METABOLITE_SUPERPATHWAY' and not exists(select keyword from deapp.de_metabolite_super_pathways msp where sk.keyword=msp.super_pathway_name || '_' || gpl_id and msp.gpl_id=gplId));
  delete from searchapp.search_keyword sk where data_category='METABOLITE_SUPERPATHWAY' and not exists(select keyword from deapp.de_metabolite_super_pathways msp where sk.keyword=msp.super_pathway_name || '_' || gpl_id and msp.gpl_id=gplId);
  delete from searchapp.search_keyword_term where search_keyword_id in (select search_keyword_id from searchapp.search_keyword sk where data_category='METABOLITE_SUBPATHWAY' and not exists(select keyword from deapp.de_metabolite_sub_pathways msp where sk.keyword=msp.sub_pathway_name || '_' || gpl_id and msp.gpl_id=gplId));
  delete from searchapp.search_keyword sk where data_category='METABOLITE_SUBPATHWAY' and not exists(select keyword from deapp.de_metabolite_sub_pathways msp where sk.keyword=msp.sub_pathway_name || '_' || gpl_id and msp.gpl_id=gplId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_sub_pway_metab',SQL%ROWCOUNT,stepCt,'Done');

		
	--	delete any existing data from peptide_annotation_deapp
	
	/*delete from mirna_annotation_deapp
	where id_ref in ( select distinct id_ref from lt_qpcr_mirna_annotation);

	

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from mirna_annotation_deapp',SQL%ROWCOUNT,stepCt,'Done');*/
        
      /*  delete from peptide_deapp
	where peptide in ( select distinct peptide from lt_metabolomic_annotation);

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from peptide_deapp',SQL%ROWCOUNT,stepCt,'Done');
        
        */
        --	delete any existing data from deapp.de_metabolite_annotation
        delete from deapp.de_metabolite_annotation
        where gpl_id = gplId;
        

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_metabolite_annotation',SQL%ROWCOUNT,stepCt,'Done');

	

	--	update organism for existing probesets in peptide_deapp
	/*
	update peptide_deapp p
	set organism=(select distinct t.organism from lt_metabolomic_annotation t
				  where p.platform = t.gpl_id
				    and p.peptide = t.peptide
                                    )
	where exists
		 (select 1 from lt_metabolomic_annotation x
		  where p.platform =x.gpl_id
		    and p.peptide = x.peptide);
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update organism in peptide_deapp',SQL%ROWCOUNT,stepCt,'Done');
		*/	 
	--	insert any new probesets into peptide_deapp
	
	/*insert into peptide_deapp
	(peptide
	,organism
	,platform)
	select distinct peptide
		  ,coalesce(organism,'Homo sapiens')
	      ,gpl_id
	from lt_metabolomic_annotation t
	where not exists
		 (select 1 from peptide_deapp x
		  where t.gpl_id = x.platform
                      and   t.peptide = x.peptide
			and coalesce(t.organism,'Homo sapiens') = coalesce(x.organism,'Homo sapiens'))
	;
        commit;
     
	--where id_ref is not null 
	--   or gene_symbol is not null;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert new probesets into peptide_deapp',SQL%ROWCOUNT,stepCt,'Done'); */

	insert into  deapp.de_metabolite_annotation
        (
        id
        ,gpl_id
        ,biochemical_name
        ,biomarker_id
        ,hmdb_id
        )
	select 
        deapp.metabolomics_annot_id.nextval
        ,d.gpl_id
	,trim(d.biochemical_name)
	,b.primary_external_id
	,d.hmdb_id
	from lt_metabolomic_annotation d
        ,bio_marker b
	--,peptide_deapp p
	where d.gpl_id = gplId
        and b.bio_marker_name(+) = d.biochemical_name;
     	  
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_annotation',SQL%ROWCOUNT,stepCt,'Done');
        
	 insert into deapp.de_metabolite_super_pathways
        (
        id
        ,gpl_id
        ,super_pathway_name
        )
	select 
        deapp.metabolite_sup_pth_id.nextval
        ,d.gpl_id
	,d.super_pathway
	from (select distinct gpl_id,super_pathway from lt_metabolomic_annotation ) d
	where d.gpl_id = gplId;
     
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_super_pathways',SQL%ROWCOUNT,stepCt,'Done');
		                   
        insert into  deapp.de_metabolite_sub_pathways
        (
        id
        ,gpl_id
        ,sub_pathway_name
        ,super_pathway_id
        )
        select 
        deapp.metabolite_sub_pth_id.nextval
        ,d.gpl_id
	,trim(d.sub_pathway)
        ,sp.id
	from (SELECT DISTINCT REGEXP_SUBSTR (sub_pathway, '[^;]+', 1, RN) AS sub_pathway ,gpl_id,super_pathway
        FROM lt_metabolomic_annotation 
        CROSS JOIN (SELECT ROWNUM AS RN 
        FROM (SELECT MAX(REGEXP_COUNT(sub_pathway,';') + 1) MX FROM lt_metabolomic_annotation) 
        CONNECT BY LEVEL <= MX) 
        WHERE REGEXP_SUBSTR (sub_pathway, '[^;]+', 1, RN) IS NOT NULL 
        ORDER BY 1) d
	,de_metabolite_super_pathways sp
	where 
        trim(d.super_pathway) = trim(sp.super_pathway_name)
        and d.gpl_id = gplId
        and sp.gpl_id = gplId;       
    
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_sub_pathways',SQL%ROWCOUNT,stepCt,'Done');
		                                                     
		
        insert into  deapp.de_metabolite_sub_pway_metab
        (
          metabolite_id
          ,sub_pathway_id
        )
	SELECT b.metabolite_id,a.ID from deapp.de_metabolite_sub_pathways a,
        (SELECT DISTINCT trim(REGEXP_SUBSTR (ss.sub_pathway, '[^;]+', 1, RN)) AS sub_pathway ,ss.metabolite_id
        FROM (select d.ID as metabolite_id,sub_pathway  from lt_metabolomic_annotation lma,deapp.de_metabolite_annotation d
        where trim(lma.biochemical_name) = trim(d.biochemical_name)
        and d.gpl_id =gplId
        and lma.gpl_id=gplId) ss 
        CROSS JOIN (SELECT ROWNUM AS RN 
        FROM (SELECT MAX(REGEXP_COUNT(sss.sub_pathway,';') + 1) MX FROM (select d.ID as metabolite_id,sub_pathway  from lt_metabolomic_annotation lma,deapp.de_metabolite_annotation d
        where trim(lma.biochemical_name) = trim(d.biochemical_name)
        and d.gpl_id =gplId
        and lma.gpl_id=gplId)sss) 
        CONNECT BY LEVEL <= MX) 
        WHERE REGEXP_SUBSTR (ss.sub_pathway, '[^;]+', 1, RN) IS NOT NULL 
        ORDER BY 2) b
        where a.gpl_id = gplId
        and a.sub_pathway_name = b.sub_pathway; 
        	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_metabolite_sub_pway_metab',SQL%ROWCOUNT,stepCt,'Done');
		
                                        
	--	update id_ref if null
	
/*	update deapp.de_peptide_annotation t
	set id_ref=(select to_number(min(b.primary_external_id)) as mirna_id
				 from biomart.mirna_bio_marker b
				 where t.mirna_symbol = b.bio_marker_name
				   and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'PROTIEN')
	where t.id_ref = idREF
	  and t.gene_id is null 
	  and t.gene_symbol is not null
	  and exists
		 (select 1 from biomart.mirna_bio_marker x
		  where t.mirna_symbol = x.bio_marker_name
			and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'metabolomic');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing id_ref in de_protien_annotation',SQL%ROWCOUNT,stepCt,'Done');
	*/
	--	update biomarker_id if null
	
        update deapp.de_metabolite_annotation t
	set biomarker_id=(select min(b.bio_marker_name) as biomarker_id
				 from biomart.mirna_bio_marker b
				 where to_char(t.biomarker_id) = b.primary_external_id
				   --and upper(b.organism) = upper(t.organism)
				   and upper(b.bio_marker_type) = 'metabolomic')
	where t.gpl_id = gplId
	  and t.biomarker_id is null
	  and t.biochemical_name is not null
	  and exists
		 (select 1 from biomart.mirna_bio_marker x
		  where to_char(t.biomarker_id) = x.primary_external_id
			--and upper(x.organism) = upper(t.organism)
			and upper(x.bio_marker_type) = 'metabolomic');
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Updated missing uniprotid in de_protien_annotation',SQL%ROWCOUNT,stepCt,'Done');
	
	--	insert probesets into biomart.bio_assay_feature_group
	
	insert into biomart.mirna_bio_assay_feature_group
	(feature_group_name
	,feature_group_type)
	select distinct t.biochemical_name, 'METABOLOMIC' --ask
	from tm_lz.lt_metabolomic_annotation t        
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
	from lt_metabolomic_annotation t
		,biomart.mirna_bio_assay_feature_group fg
		,biomart.mirna_bio_marker bgs
		,biomart.mirna_bio_marker bgi
	where (t.hmdb_id is not null)
	  and t.biochemical_name = fg.feature_group_name
	  and t.biochemical_name = bgs.bio_marker_name(+)
	  --and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgs.organism)
	  and to_char(t.hmdb_id) = bgi.primary_external_id(+)
	  --and upper(coalesce(t.organism,'Homo sapiens')) = upper(bgi.organism)
	  and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) > 0
	  and not exists
		 (select 1 from biomart.mirna_bio_assay_data_annot x
		  where fg.bio_assay_feature_group_id = x.bio_assay_feature_group_id
		    and coalesce(bgs.bio_marker_id,bgi.bio_marker_id,-1) = x.bio_marker_id);
			
	stepCt := stepCt + 1; 
	cz_write_audit(jobId,databaseName,procedureName,'Link feature_group to bio_marker in biomart.mirna_bio_assay_data_annotation',SQL%ROWCOUNT,stepCt,'Done');
			
        -- Inserts subpathways into search_keyword,
-- subpathways into search_keyword_term,
-- superpathways into search_keyword,
-- superpathways into search_keyword_term

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

        stepCt := stepCt + 1; 
	cz_write_audit(jobId,databaseName,procedureName,'Insert subpathways into search_keyword',SQL%ROWCOUNT,stepCt,'Done');
	

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
                    FROM deapp.de_metabolite_sub_pathways subp
                    where subp.gpl_id = gplId
                )
            );

            stepCt := stepCt + 1; 
            cz_write_audit(jobId,databaseName,procedureName,'Insert subpathways into search_keyword_term',SQL%ROWCOUNT,stepCt,'Done');
            
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

            stepCt := stepCt + 1; 
            cz_write_audit(jobId,databaseName,procedureName,'Insert superpathways into search_keyword',SQL%ROWCOUNT,stepCt,'Done');
            
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
            );

        stepCt := stepCt + 1; 
        cz_write_audit(jobId,databaseName,procedureName,'Insert superpathways into search_keyword_term',SQL%ROWCOUNT,stepCt,'Done');
                        
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
          annotation.hmdb_id IS NULL
          and CONCAT('PRIVATE:', annotation.id) not in (
          select primary_external_id from biomart.bio_marker);
          
        stepCt := stepCt + 1; 
        cz_write_audit(jobId,databaseName,procedureName,'Insert into biomart.bio_marker',SQL%ROWCOUNT,stepCt,'Done');
                 
        UPDATE deapp.de_metabolite_annotation annotation
        SET  annotation.hmdb_id = CONCAT('PRIVATE:', annotation.id)
        WHERE  annotation.hmdb_id IS NULL;
                 
        stepCt := stepCt + 1; 
        cz_write_audit(jobId,databaseName,procedureName,'Update deapp.de_metabolite_annotation',SQL%ROWCOUNT,stepCt,'Done');                 
        
        stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End I2B2_LOAD_METABOLOMICS_ANNOT',0,stepCt,'Done');
        
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
 
