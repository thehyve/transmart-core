--
-- Name: modifier_dimension_view; Type: VIEW; Schema: i2b2demodata; Owner: -
--
CREATE VIEW modifier_dimension_view AS
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
   FROM (modifier_dimension md
     LEFT JOIN modifier_metadata mm ON (((md.modifier_cd)::text = (mm.modifier_cd)::text)));

