--
-- Type: VIEW; Owner: I2B2DEMODATA; Name: MODIFIER_DIMENSION_VIEW
--
CREATE OR REPLACE FORCE VIEW "I2B2DEMODATA"."MODIFIER_DIMENSION_VIEW" ("MODIFIER_PATH", "MODIFIER_CD", "NAME_CHAR", "MODIFIER_BLOB", "UPDATE_DATE", "DOWNLOAD_DATE", "IMPORT_DATE", "SOURCESYSTEM_CD", "UPLOAD_ID", "MODIFIER_LEVEL", "MODIFIER_NODE_TYPE", "VALTYPE_CD", "STD_UNITS", "VISIT_IND") AS 
SELECT md.modifier_path,
  md.modifier_cd,
  md.name_char,
  md.modifier_blob,
  md.update_date,
  md.download_date,
  md.import_date,
  md.sourcesystem_cd,
  md.upload_id,
  md.modifier_level,
  md.modifier_node_type,
  mm.valtype_cd,
  mm.std_units,
  mm.visit_ind
 FROM i2b2demodata.modifier_dimension md
   LEFT JOIN i2b2demodata.modifier_metadata mm ON md.modifier_cd = mm.modifier_cd;

