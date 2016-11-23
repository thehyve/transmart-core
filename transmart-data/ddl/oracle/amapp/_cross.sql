--
-- Type: VIEW; Owner: AMAPP; Name: AM_TAG_DISPLAY_VW
--
  CREATE OR REPLACE FORCE VIEW "AMAPP"."AM_TAG_DISPLAY_VW" ("SUBJECT_UID", "TAG_ITEM_ID", "DISPLAY_VALUE", "OBJECT_TYPE", "OBJECT_UID", "OBJECT_ID") AS 
  SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    TO_CHAR(tval.value) AS display_value,
    tass.object_type,
    tass.object_uid    AS object_uid,
    obj_uid.am_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN AMAPP.am_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN AMAPP.am_tag_value tval
  ON obj_uid.am_data_id = tval.tag_value_id
  UNION
  -- BIO_CONCEPT_CODE
  SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    TO_CHAR(bio_val.code_name) AS display_value,
    tass.object_type,
    tass.object_uid     AS object_uid,
    obj_uid.bio_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN biomart.bio_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN BIOMART.bio_concept_code bio_val
  ON obj_uid.bio_data_id = bio_val.bio_concept_code_id
  UNION
  -- BIO_DISEASE
  SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    TO_CHAR(bio_val.disease) AS display_value,
    tass.object_type,
    tass.object_uid     AS object_uid,
    obj_uid.bio_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN biomart.bio_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN BIOMART.bio_disease bio_val
  ON obj_uid.bio_data_id = bio_val.bio_disease_id
  UNION
  -- BIO_ASSAY_PLATFORM
SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    case when ati.code_type_name = 'PLATFORM_NAME'
    then TO_CHAR(bio_val.platform_type)||'/'||TO_CHAR(bio_val.platform_technology)|| '/' ||TO_CHAR(bio_val.platform_vendor)|| '/' ||TO_CHAR(bio_val.platform_name) 
    when ati.code_type_name = 'VENDOR' then TO_CHAR(bio_val.platform_vendor) 
    when ati.code_type_name = 'MEASUREMENT_TYPE' then TO_CHAR(bio_val.platform_type) 
    when ati.code_type_name = 'TECHNOLOGY' then TO_CHAR(bio_val.platform_technology) end AS display_value,
    tass.object_type,
    tass.object_uid     AS object_uid,
    obj_uid.bio_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN biomart.bio_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN BIOMART.bio_assay_platform bio_val
  ON obj_uid.bio_data_id = bio_val.bio_assay_platform_id
  JOIN amapp.am_tag_item ati 
  ON ati.tag_item_id = tass.tag_item_id
UNION
  -- BIO_COMPOUND
  SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    TO_CHAR(bio_val.code_name) AS display_value,
    tass.object_type,
    tass.object_uid     AS object_uid,
    obj_uid.bio_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN biomart.bio_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN BIOMART.bio_compound bio_val
  ON obj_uid.bio_data_id = bio_val.bio_compound_id

    UNION
  -- BIO_MARKER
  SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    TO_CHAR(bio_val.bio_marker_name) AS display_value,
    tass.object_type,
    tass.object_uid     AS object_uid,
    obj_uid.bio_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN biomart.bio_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN BIOMART.bio_marker bio_val
  ON obj_uid.bio_data_id = bio_val.bio_marker_id
  
     UNION
  -- BIO_OBSERVATION
  SELECT DISTINCT tass.subject_uid,
    tass.tag_item_id,
    TO_CHAR(bio_val.obs_name) AS display_value,
    tass.object_type,
    tass.object_uid     AS object_uid,
    obj_uid.bio_data_id AS object_id
  FROM AMAPP.am_tag_association tass
  JOIN biomart.bio_data_uid obj_uid
  ON tass.object_uid = obj_uid.unique_id
  JOIN BIOMART.bio_observation bio_val
ON obj_uid.bio_data_id = bio_val.bio_observation_id;
 
