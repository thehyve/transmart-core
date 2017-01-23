--
-- Name: i2b2_load_annotation_deapp(numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_load_annotation_deapp(currentjobid numeric DEFAULT (-1)) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/
Declare
 
	--Audit variables
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	character varying;
	rtnCd			integer;

	gplId			character varying;

BEGIN

	stepCt := 0;

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_ANNOTATION_DEAPP';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it

	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobID;
	END IF;

	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_load_annotation_deapp',0,stepCt,'Done') into rtnCd;

	--	get GPL id from external table

	select distinct gpl_id into gplId from tm_lz.lt_src_deapp_annot;

	--	delete any existing data from annotation_deapp

	begin
	delete from tm_cz.annotation_deapp
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from annotation_deapp',rowCt,stepCt,'Done') into rtnCd;

	--	delete any existing data from deapp.de_mrna_annotation

	begin
	delete from deapp.de_mrna_annotation
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from de_mrna_annotation',rowCt,stepCt,'Done') into rtnCd;

	--	update organism for existing probesets in probeset_deapp

	begin
	with upd as (select distinct t.gpl_id, t.probe_id, t.organism from tm_lz.lt_src_deapp_annot t)
	update tm_cz.probeset_deapp
	set organism=upd.organism
	from upd
	where platform = upd.gpl_id
	  and probeset = upd.probe_id
	and exists
		 (select 1 from tm_lz.lt_src_deapp_annot x
		  where platform = x.gpl_id
		    and probeset = x.probe_id);
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Update organism in probeset_deapp',rowCt,stepCt,'Done') into rtnCd;

	--	insert any new probesets into probeset_deapp

	begin
	insert into tm_cz.probeset_deapp
	(probeset
	,organism
	,platform)
	select distinct probe_id
		  ,coalesce(organism,'Homo sapiens')
	      ,gpl_id
	from tm_lz.lt_src_deapp_annot t
	where not exists
		 (select 1 from tm_cz.probeset_deapp x
		  where t.gpl_id = x.platform
		    and t.probe_id = x.probeset
			and coalesce(t.organism,'Homo sapiens') = coalesce(x.organism,'Homo sapiens'));
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert new probesets into probeset_deapp',rowCt,stepCt,'Done') into rtnCd;

	--	insert data into annotation_deapp

	begin
	insert into tm_cz.annotation_deapp
	(gpl_id
	,probe_id
	,gene_symbol
	,gene_id
	,probeset_id
	,organism)
	select distinct d.gpl_id
	,d.probe_id
	,d.gene_symbol
	,d.gene_id
	,p.probeset_id
	,coalesce(d.organism,'Homo sapiens')
	from tm_lz.lt_src_deapp_annot d
	,tm_cz.probeset_deapp p
	where d.probe_id = p.probeset
	  and d.gpl_id = p.platform
	  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens');
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into REFERENCE annotation_deapp',rowCt,stepCt,'Done') into rtnCd;

	--	insert data into deapp.de_mrna_annotation

	begin
	insert into deapp.de_mrna_annotation
	(gpl_id
	,probe_id
	,gene_symbol
	,gene_id
	,probeset_id
	,organism)
	select distinct d.gpl_id
	,d.probe_id
	,d.gene_symbol
	,case when d.gene_id is null then null when d.gene_id='' then null else d.gene_id::numeric end as gene_id
	,p.probeset_id
	,coalesce(d.organism,'Homo sapiens')
	from tm_lz.lt_src_deapp_annot d
	,tm_cz.probeset_deapp p
	where d.probe_id = p.probeset
	  and d.gpl_id = p.platform
	  and coalesce(d.organism,'Homo sapiens') = coalesce(p.organism,'Homo sapiens');
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load annotation data into DEAPP de_mrna_annotation',rowCt,stepCt,'Done') into rtnCd;

	--	update gene_id if null

	begin
	with upd as (select b.bio_marker_name as gene_symbol, b.organism, min(b.primary_external_id::numeric) as gene_id 
				 from biomart.bio_marker b
				 where upper(b.bio_marker_type) = 'GENE'
				 group by b.bio_marker_name, b.organism)
	update deapp.de_mrna_annotation a
	set gene_id=upd.gene_id
	from upd
	where a.gpl_id = gplId
	  and a.gene_id is null
	  and a.gene_symbol is not null
	  and a.gene_symbol = upd.gene_symbol
	  and upper(a.organism) = upper(upd.organism)
	  and exists
	      (select 1 from biomart.bio_marker x
              where a.gene_symbol = x.bio_marker_name
                and upper(x.organism) = upper(a.organism)
             	and upper(x.bio_marker_type) = 'GENE');
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Updated missing gene_id in de_mrna_annotation',rowCt,stepCt,'Done') into rtnCd;

	--	update gene_symbol if null

	begin
	with upd as (select b.primary_external_id::numeric as gene_id, b.organism, min(b.bio_marker_name) as gene_symbol
				 from biomart.bio_marker b
				 where upper(b.bio_marker_type) = 'GENE'
				 group by b.primary_external_id, b.organism)
	update deapp.de_mrna_annotation a
	set gene_symbol=upd.gene_symbol
	from upd
	where a.gpl_id = gplId
	  and a.gene_symbol is null
	  and a.gene_id is not null
	  and a.gene_id = upd.gene_id
	  and upper(a.organism) = upper(upd.organism)
	  and exists
	      (select 1 from biomart.bio_marker x
              where a.gene_symbol = x.bio_marker_name
                and upper(x.organism) = upper(a.organism)
             	and upper(x.bio_marker_type) = 'GENE');
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Updated missing gene_symbol in de_mrna_annotation',rowCt,stepCt,'Done') into rtnCd;

	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'End i2b2_load_annotation_deapp',0,stepCt,'Done') into rtnCd;

       --Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;

	return 1;

END;

$$;

