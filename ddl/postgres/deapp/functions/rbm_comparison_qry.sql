--
-- Name: rbm_comparison_qry(character varying, character varying, character varying, refcursor); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION rbm_comparison_qry(patient_ids character varying, concept_cds character varying, timepoints character varying, INOUT cv_1 refcursor) RETURNS refcursor
    LANGUAGE plpgsql
    AS $$
DECLARE


  tp_cnt integer;

BEGIN
  -------------------------------------------------------------------------------
   -- Returns ANTIGENE, GENE_SYMBOL, VALUE, PATIENT_ID, ASSAY_ID
   -- result set for specifed Pathways filtered
   -- by Sample Types and Subject_id.
   -- HX@20090317 - First rev.
   -- HX@20090319 - Rename DE_RBM_DATA to DE_SUBJECT_RBM_DATA and move CONCEPT_CD
   --     from DE_RBM_DATA to DE_SUBJECT_SAMPLE_MAPPING, and make the query
   --     and table names are  consistent with MicroArray's one

   -- 2009-06-18: change normalized_value to zscore
   -- 2009-06-23: Add timepoints as parameter
   -- 2009-06-30: Remove t1.zscore is not null condition
   -------------------------------------------------------------------------------

   select count(*) into tp_cnt
   from de_subject_sample_mapping
   where timepoint_cd in
       (select * from table(text_parser(timepoints)));


   if (tp_cnt=0) then
        OPEN cv_1 FOR
           SELECT distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                 t1.patient_id, t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1
           where -- t1.zscore is not null and
              t1.patient_id IN (SELECT * from table(text_parser(patient_ids)))
               order by t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.patient_id;
   else
        OPEN cv_1 FOR
           SELECT distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                  t1.patient_id, t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1, de_subject_sample_mapping t2
           where --t1.zscore is not null and
              t2.patient_id IN (SELECT * from table(text_parser(patient_ids))) and
              t2.timepoint_cd in (select * from TABLE(text_parser(timepoints))) and
              t1.data_uid = t2.data_uid and t1.assay_id=t2.assay_id
              order by t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.patient_id;
    end if;
END RBM_comparison_qry;
 
 
 
$$;

