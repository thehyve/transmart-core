--
-- Name: protein_dbl_comp_qry(character varying, character varying, character varying, character varying, character varying, character varying, character varying, character varying, refcursor); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION protein_dbl_comp_qry(patient_ids1 character varying, patient_ids2 character varying, sample_types1 character varying, sample_types2 character varying, pathway_uid1 character varying, pathway_uid2 character varying, timepoints1 character varying, timepoints2 character varying, INOUT cv_1 refcursor) RETURNS refcursor
    LANGUAGE plpgsql
    AS $$
DECLARE

  sample_record_count1 integer;
  sample_record_count2 integer;
  timepoint_count1 integer;
  timepoint_count2 integer;

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
          SELECT distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          SELECT distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
          order by GENE_SYMBOL, COMPONENT, patient_ID ;
      elsif ((timepoint_count1>0) and (timepoint_count2=0)) then
          OPEN cv_1 FOR
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S2_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids2)))
          order by GENE_SYMBOL, COMPONENT, patient_ID ;

      elsif ((timepoint_count1=0) and (timepoint_count2>0)) then
          OPEN cv_1 FOR
          SELECT distinct a.component, a.GENE_SYMBOL, a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
              FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types1))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL,  a.zscore,
                 'S1_' || a.patient_ID as patient_id, a.ASSAY_ID, a.intensity
          FROM DE_SUBJECT_PROTEIN_DATA a, DE_pathway_gene c, de_pathway p,
               DE_subject_sample_mapping b
          where p.pathway_uid= pathway_uid1 and c.pathway_id= p.id and
                a.gene_symbol = c.gene_symbol and
                a.PATIENT_ID = b.PATIENT_ID and a.assay_id = b.assay_id and
                b.concept_code IN (SELECT * from table(text_parser(sample_types1))) and
                a.patient_id IN (SELECT * from table(text_parser(patient_ids1)))
          UNION
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
          SELECT distinct a.COMPONENT, a.GENE_SYMBOL, a.zscore,
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
 
 
 
$$;

