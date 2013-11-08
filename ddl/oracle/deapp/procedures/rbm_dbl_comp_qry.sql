--
-- Type: PROCEDURE; Owner: DEAPP; Name: RBM_DBL_COMP_QRY
--
  CREATE OR REPLACE PROCEDURE "DEAPP"."RBM_DBL_COMP_QRY" 
(
  patient_ids1 IN VARCHAR2
, patient_ids2 IN VARCHAR2
, concept_cds1 IN VARCHAR2
, concept_cds2 IN VARCHAR2
, timepoints1 IN VARCHAR2
, timepoints2 IN VARCHAR2
, cv_1 IN OUT SYS_REFCURSOR
) AS
  tp_cnt1 INTEGER;
  tp_cnt2 INTEGER;
BEGIN
   select count(*) into tp_cnt1
   from de_subject_sample_mapping
   where timepoint_cd in
       (select * from table(text_parser(timepoints1)));

   select count(*) into tp_cnt2
   from de_subject_sample_mapping
   where timepoint_cd in
       (select * from table(text_parser(timepoints2)));


   if ((tp_cnt1=0) and (tp_cnt2=0)) then
        OPEN cv_1 FOR
           select distinct ANTIGEN_NAME, GENE_SYMBOL, zscore as value,
                 'S1_' || patient_id as patient_id, assay_id
           FROM DE_SUBJECT_RBM_DATA
           where -- t1.zscore is not null and
              patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          select distinct ANTIGEN_NAME, GENE_SYMBOL, zscore as value,
                 'S2_' || patient_id as patient_id, assay_id
           FROM DE_SUBJECT_RBM_DATA t1
           where -- t1.zscore is not null and
              patient_id IN (SELECT * from table(text_parser(patient_ids2)))
               order by ANTIGEN_NAME, GENE_SYMBOL, patient_id;
   elsif ((tp_cnt1=0) and (tp_cnt2 > 0)) then
           OPEN cv_1 FOR
           select distinct ANTIGEN_NAME, GENE_SYMBOL, zscore as value,
                 'S1_' || patient_id as patient_id, assay_id
           FROM DE_SUBJECT_RBM_DATA t1
           where -- zscore is not null and
              patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
           select distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                  'S1_' || t1.patient_id as patient_id, t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1, de_subject_sample_mapping t2
           where --t1.zscore is not null and
              t2.patient_id IN (SELECT * from table(text_parser(patient_ids2))) and
              t2.timepoint_cd in (select * from TABLE(text_parser(timepoints2))) and
              t1.data_uid = t2.data_uid and t1.assay_id=t2.assay_id
              order by ANTIGEN_NAME, GENE_SYMBOL, patient_id;

   elsif ((tp_cnt1 > 0 ) and (tp_cnt2 = 0)) then
        OPEN cv_1 FOR
           select distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                  'S1_' || t1.patient_id as patient_id  , t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1, de_subject_sample_mapping t2
           where --t1.zscore is not null and
              t2.patient_id IN (SELECT * from table(text_parser(patient_ids1))) and
              t2.timepoint_cd in (select * from TABLE(text_parser(timepoints1))) and
              t1.data_uid = t2.data_uid and t1.assay_id=t2.assay_id
          UNION
          select distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                 'S2_' || patient_id as patient_id, t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1
           where -- t1.zscore is not null and
              t1.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
               order by ANTIGEN_NAME, GENE_SYMBOL, patient_id;
   else
        OPEN cv_1 FOR
          select distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                  'S1_' || t1.patient_id as patient_id, t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1, de_subject_sample_mapping t2
           where --t1.zscore is not null and
              t2.patient_id IN (SELECT * from table(text_parser(patient_ids1))) and
              t2.timepoint_cd in (select * from TABLE(text_parser(timepoints1))) and
              t1.data_uid = t2.data_uid and t1.assay_id=t2.assay_id
          UNION
          select distinct t1.ANTIGEN_NAME, t1.GENE_SYMBOL, t1.zscore as value,
                  'S2_' || t1.patient_id as patient_id, t1.assay_id
           FROM DE_SUBJECT_RBM_DATA t1, de_subject_sample_mapping t2
           where --t1.zscore is not null and
              t2.patient_id IN (SELECT * from table(text_parser(patient_ids2))) and
              t2.timepoint_cd in (select * from TABLE(text_parser(timepoints2))) and
              t1.data_uid = t2.data_uid and t1.assay_id=t2.assay_id
              order by ANTIGEN_NAME, GENE_SYMBOL, patient_id;
    end if;

END RBM_DBL_COMP_QRY;
 
 
/
 
