--
-- Type: PROCEDURE; Owner: DEAPP; Name: PROTEIN_DBL_COMP_QRY
--
  CREATE OR REPLACE PROCEDURE "DEAPP"."PROTEIN_DBL_COMP_QRY" 
(
  patient_ids1 IN VARCHAR2
, patient_ids2 IN VARCHAR2
, sample_types1 IN VARCHAR2
, sample_types2 IN VARCHAR2
, pathway_uid1 IN VARCHAR2
, pathway_uid2 IN VARCHAR2
, timepoints1 IN VARCHAR2
, timepoints2 IN VARCHAR2
, cv_1 IN OUT SYS_REFCURSOR
) AS
  sample_record_count1 INTEGER;
  sample_record_count2 INTEGER;
  timepoint_count1 INTEGER;
  timepoint_count2 INTEGER;
BEGIN
-- Check if sample Types Exist
SELECT COUNT(*)
  INTO sample_record_count1
  FROM DE_SUBJECT_SAMPLE_MAPPING
    WHERE concept_code IN
      --Passing string to Text parser Function
      (SELECT * from table(text_parser(sample_types1)));

SELECT COUNT(*)
  INTO sample_record_count2
  FROM DE_SUBJECT_SAMPLE_MAPPING
    WHERE concept_code IN
      --Passing string to Text parser Function
      (SELECT * from table(text_parser(sample_types2)));

 --Sample Record Count is invalid or non existent.
  IF sample_record_count1 = 0
    THEN
    BEGIN

      select count(*) into timepoint_count1
      from table(text_parser(timepoints1));

      select count(*) into timepoint_count2
      from table(text_parser(timepoints2));

      if ((timepoint_count1=0) and (timepoint_count2=0)) then

          OPEN cv_1 FOR
          select distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          select distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
          order by GENE_SYMBOL, COMPONENT, patient_ID ;
      elsif ((timepoint_count1>0) and (timepoint_count2=0)) then
          OPEN cv_1 FOR
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints1))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint and
                a.assay_id=b.assay_id
          UNION
          select distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
          order by GENE_SYMBOL, COMPONENT, patient_ID ;

      elsif ((timepoint_count1=0) and (timepoint_count2>0)) then
          OPEN cv_1 FOR
          select distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints2))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint and
                a.assay_id=b.assay_id
          order by GENE_SYMBOL, COMPONENT, patient_ID ;

      else

          OPEN cv_1 FOR
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints1))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint and
                a.assay_id=b.assay_id
          UNION
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints2))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint and
                a.assay_id=b.assay_id
          order by GENE_SYMBOL, COMPONENT, patient_ID;
      end if;
    END;

  --else use all filters (If Subject is non existent or invalid, then return
  ELSE
    BEGIN

    if ((timepoint_count1=0) and (timepoint_count2=0)) then
      OPEN cv_1 FOR
          select distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types1))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          select distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types2))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
          order by GENE_SYMBOL, COMPONENT, patient_ID;
    elsif ((timepoint_count1>0) and (timepoint_count2=0)) then
      OPEN cv_1 FOR
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types1))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints1))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint
          UNION
          select distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types2))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
          order by GENE_SYMBOL, COMPONENT, patient_ID;
    elsif ((timepoint_count1=0) and (timepoint_count2>0)) then
      OPEN cv_1 FOR
          select distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types1))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types2))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints2))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint
          order by GENE_SYMBOL, COMPONENT, patient_ID;
    else

      OPEN cv_1 FOR
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types1))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints1))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint
          UNION
          select distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
                 'S2' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types2))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2))) and
                b.TIMEPOINT_CD IN (SELECT * from table(text_parser(timepoints2))) and
                a.PATIENT_ID=b.patient_id and a.timepoint=b.timepoint
          order by GENE_SYMBOL, COMPONENT, patient_ID;

    end if;

    END;
  END IF;

END PROTEIN_DBL_COMP_QRY;
/
 
