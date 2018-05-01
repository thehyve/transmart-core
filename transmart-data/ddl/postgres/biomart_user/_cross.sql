--
-- Name: bio_data_values_view; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.bio_data_values_view AS
 SELECT bdu.unique_id,
    COALESCE(bcc.code_name, bdi.disease, bo.obs_name, bm.bio_marker_name, bap.platform_name, bc.generic_name, bc.brand_name, bc.jnj_number, bc.cnto_number) AS name,
    COALESCE(bcc.code_description, bo.obs_descr, bm.bio_marker_description, bap.platform_description, bc.description) AS description
   FROM ((((((biomart.bio_data_uid bdu
     LEFT JOIN biomart.bio_concept_code bcc ON ((((bdu.bio_data_type)::text = 'BIO_CONCEPT_CODE'::text) AND (bdu.bio_data_id = bcc.bio_concept_code_id))))
     LEFT JOIN biomart.bio_disease bdi ON ((((bdu.bio_data_type)::text = 'BIO_DISEASE'::text) AND (bdu.bio_data_id = bdi.bio_disease_id))))
     LEFT JOIN biomart.bio_observation bo ON ((((bdu.bio_data_type)::text = 'BIO_OBSERVATION'::text) AND (bdu.bio_data_id = bo.bio_observation_id))))
     LEFT JOIN biomart.bio_marker bm ON ((((bdu.bio_data_type)::text ~~ 'BIO_MARKER.%'::text) AND (bdu.bio_data_id = bm.bio_marker_id))))
     LEFT JOIN biomart.bio_assay_platform bap ON ((((bdu.bio_data_type)::text = 'BIO_ASSAY_PLATFORM'::text) AND (bdu.bio_data_id = bap.bio_assay_platform_id))))
     LEFT JOIN biomart.bio_compound bc ON ((((bdu.bio_data_type)::text = 'BIO_COMPOUND'::text) AND (bdu.bio_data_id = bc.bio_compound_id))))
UNION
 SELECT adu.unique_id,
    atv.value AS name,
    NULL::character varying AS description
   FROM (amapp.am_data_uid adu
     JOIN amapp.am_tag_value atv ON ((adu.am_data_id = atv.tag_value_id)));

--
-- Name: browse_analyses_view; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.browse_analyses_view AS
 SELECT fd.unique_id AS id,
    baa.analysis_name AS title,
    baa.long_description AS description,
    string_agg((bap.platform_type)::text, '|'::text ORDER BY (bap.platform_type)::text) AS measurement_type,
    string_agg((bap.platform_name)::text, '|'::text ORDER BY (bap.platform_name)::text) AS platform_name,
    string_agg((bap.platform_vendor)::text, '|'::text ORDER BY (bap.platform_vendor)::text) AS vendor,
    string_agg((bap.platform_technology)::text, '|'::text ORDER BY (bap.platform_technology)::text) AS technology
   FROM (((((((biomart.bio_assay_analysis baa
     JOIN biomart.bio_data_uid bd ON ((baa.bio_assay_analysis_id = bd.bio_data_id)))
     JOIN fmapp.fm_folder_association fa ON (((fa.object_uid)::text = (bd.unique_id)::text)))
     JOIN fmapp.fm_data_uid fd ON ((fa.folder_id = fd.fm_data_id)))
     JOIN fmapp.fm_folder ff ON ((ff.folder_id = fa.folder_id)))
     LEFT JOIN amapp.am_tag_association ata ON (((fd.unique_id)::text = (ata.subject_uid)::text)))
     LEFT JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
     LEFT JOIN biomart.bio_assay_platform bap ON ((bap.bio_assay_platform_id = bdu.bio_data_id)))
  WHERE (((ata.object_type)::text = 'BIO_ASSAY_PLATFORM'::text) AND ff.active_ind)
  GROUP BY fd.unique_id, baa.analysis_name, baa.long_description;

--
-- Name: browse_assays_view; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.browse_assays_view AS
 SELECT DISTINCT fd.unique_id AS id,
    f.folder_name AS title,
    f.description,
    string_agg((bap.platform_type)::text, '|'::text ORDER BY (bap.platform_type)::text) AS measurement_type,
    string_agg((bap.platform_name)::text, '|'::text ORDER BY (bap.platform_name)::text) AS platform_name,
    string_agg((bap.platform_vendor)::text, '|'::text ORDER BY (bap.platform_vendor)::text) AS vendor,
    string_agg((bap.platform_technology)::text, '|'::text ORDER BY (bap.platform_technology)::text) AS technology,
    bio_markers.object_uids AS gene,
    bio_markers.object_uids AS mirna,
    biomarker_types.object_uids AS biomarker_type
   FROM ((((((fmapp.fm_folder f
     JOIN fmapp.fm_data_uid fd ON ((f.folder_id = fd.fm_data_id)))
     LEFT JOIN amapp.am_tag_association ata ON ((((fd.unique_id)::text = (ata.subject_uid)::text) AND ((ata.object_type)::text = 'BIO_ASSAY_PLATFORM'::text))))
     LEFT JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
     LEFT JOIN biomart.bio_assay_platform bap ON ((bap.bio_assay_platform_id = bdu.bio_data_id)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata_1.object_uid)::text, '|'::text ORDER BY (ata_1.object_uid)::text) AS object_uids
           FROM ((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata_1 ON (((fdu.unique_id)::text = (ata_1.subject_uid)::text)))
          WHERE (((ata_1.object_type)::text = 'BIO_MARKER'::text) AND ((ff.folder_type)::text = 'ASSAY'::text))
          GROUP BY fdu.unique_id) bio_markers ON (((bio_markers.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata_1.object_uid)::text, '|'::text ORDER BY (ata_1.object_uid)::text) AS object_uids
           FROM (((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata_1 ON (((fdu.unique_id)::text = (ata_1.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata_1.tag_item_id = ati.tag_item_id)))
          WHERE ((((ata_1.object_type)::text = 'BIO_CONCEPT_CODE'::text) AND ((ati.code_type_name)::text = 'ASSAY_TYPE_OF_BM_STUDIED'::text)) AND ((ff.folder_type)::text = 'ASSAY'::text))
          GROUP BY fdu.unique_id) biomarker_types ON (((biomarker_types.id)::text = (fd.unique_id)::text)))
  WHERE (((f.folder_type)::text = 'ASSAY'::text) AND f.active_ind)
  GROUP BY fd.unique_id, f.folder_name, f.description, bio_markers.object_uids, biomarker_types.object_uids;

--
-- Name: browse_folders_view; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.browse_folders_view AS
 SELECT fd.unique_id AS id,
    f.folder_name AS title,
    f.description,
    string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS file_type
   FROM ((fmapp.fm_folder f
     JOIN fmapp.fm_data_uid fd ON ((f.folder_id = fd.fm_data_id)))
     LEFT JOIN amapp.am_tag_association ata ON (((fd.unique_id)::text = (ata.subject_uid)::text)))
  WHERE (((((f.folder_type)::text = 'FOLDER'::text) AND f.active_ind) AND ((ata.object_type)::text = 'BIO_CONCEPT_CODE'::text)) AND ((ata.object_uid)::text ~~ 'FILE_TYPE%'::text))
  GROUP BY fd.unique_id, f.folder_name, f.description;

--
-- Name: browse_programs_view; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.browse_programs_view AS
 SELECT fd.unique_id AS id,
    f.folder_name AS title,
    f.description,
    diseases.object_uids AS disease,
    observations.object_uids AS observation,
    pathways.object_uids AS pathway,
    genes.object_uids AS gene,
    therapeutic_domains.object_uids AS therapeutic_domain,
    institutions.object_uids AS institution,
    targets.object_uids AS target
   FROM ((((((((fmapp.fm_folder f
     JOIN fmapp.fm_data_uid fd ON ((f.folder_id = fd.fm_data_id)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM ((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_disease bd ON ((bd.bio_disease_id = bdu.bio_data_id)))
          WHERE (((ata.object_type)::text = ANY (ARRAY[('BIO_DISEASE'::character varying)::text, ('PROGRAM_TARGET'::character varying)::text])) AND ((ff.folder_type)::text = 'PROGRAM'::text))
          GROUP BY fdu.unique_id) diseases ON (((diseases.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM ((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_observation bo ON ((bo.bio_observation_id = bdu.bio_data_id)))
          WHERE (((ata.object_type)::text = ANY (ARRAY[('BIO_OBSERVATION'::character varying)::text, ('PROGRAM_TARGET'::character varying)::text])) AND ((ff.folder_type)::text = 'PROGRAM'::text))
          GROUP BY fdu.unique_id) observations ON (((observations.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM ((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_marker bm ON ((bm.bio_marker_id = bdu.bio_data_id)))
          WHERE ((((bm.bio_marker_type)::text = 'PATHWAY'::text) AND (((ata.object_type)::text = 'BIO_MARKER'::text) OR ((ata.object_type)::text = 'PROGRAM_TARGET'::text))) AND ((ff.folder_type)::text = 'PROGRAM'::text))
          GROUP BY fdu.unique_id) pathways ON (((pathways.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM ((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_marker bm ON ((bm.bio_marker_id = bdu.bio_data_id)))
          WHERE ((((bm.bio_marker_type)::text = 'GENE'::text) AND (((ata.object_type)::text = 'BIO_MARKER'::text) OR ((ata.object_type)::text = 'PROGRAM_TARGET'::text))) AND ((ff.folder_type)::text = 'PROGRAM'::text))
          GROUP BY fdu.unique_id) genes ON (((genes.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM (((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata.tag_item_id = ati.tag_item_id)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_concept_code bcc ON ((bcc.bio_concept_code_id = bdu.bio_data_id)))
          WHERE ((((ata.object_type)::text = ANY (ARRAY[('BIO_CONCEPT_CODE'::character varying)::text, ('PROGRAM_TARGET'::character varying)::text])) AND ((ff.folder_type)::text = 'PROGRAM'::text)) AND ((ati.code_type_name)::text = 'THERAPEUTIC_DOMAIN'::text))
          GROUP BY fdu.unique_id) therapeutic_domains ON (((therapeutic_domains.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM (((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata.tag_item_id = ati.tag_item_id)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_concept_code bcc ON ((bcc.bio_concept_code_id = bdu.bio_data_id)))
          WHERE ((((ata.object_type)::text = ANY (ARRAY[('BIO_CONCEPT_CODE'::character varying)::text, ('PROGRAM_TARGET'::character varying)::text])) AND ((ff.folder_type)::text = 'PROGRAM'::text)) AND ((ati.code_type_name)::text = 'PROGRAM_INSTITUTION'::text))
          GROUP BY fdu.unique_id) institutions ON (((institutions.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM (((((fmapp.fm_folder ff
             JOIN fmapp.fm_data_uid fdu ON ((ff.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata.tag_item_id = ati.tag_item_id)))
             JOIN biomart.bio_data_uid bdu ON (((bdu.unique_id)::text = (ata.object_uid)::text)))
             JOIN biomart.bio_concept_code bcc ON ((bcc.bio_concept_code_id = bdu.bio_data_id)))
          WHERE ((((ata.object_type)::text = ANY (ARRAY[('BIO_CONCEPT_CODE'::character varying)::text, ('PROGRAM_TARGET'::character varying)::text])) AND ((ff.folder_type)::text = 'PROGRAM'::text)) AND ((ati.code_type_name)::text = 'PROGRAM_TARGET_PATHWAY_PHENOTYPE'::text))
          GROUP BY fdu.unique_id) targets ON (((targets.id)::text = (fd.unique_id)::text)))
  WHERE (((f.folder_type)::text = 'PROGRAM'::text) AND f.active_ind);

--
-- Name: browse_studies_view; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.browse_studies_view AS
 SELECT fd.unique_id AS id,
    exp.title,
    exp.description,
    exp.design,
    exp.biomarker_type,
    exp.access_type,
    exp.accession,
    exp.institution,
    exp.country,
    diseases.object_uids AS disease,
    compounds.object_uids AS compound,
    study_objectives.object_uids AS study_objective,
    species.object_uids AS organism,
    phases.object_uids AS study_phase
   FROM (((((((((biomart.bio_experiment exp
     JOIN biomart.bio_data_uid bd ON ((exp.bio_experiment_id = bd.bio_data_id)))
     JOIN fmapp.fm_folder_association fa ON (((fa.object_uid)::text = (bd.unique_id)::text)))
     JOIN fmapp.fm_data_uid fd ON ((fa.folder_id = fd.fm_data_id)))
     JOIN fmapp.fm_folder ff ON ((ff.folder_id = fa.folder_id)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM ((fmapp.fm_folder_association ffa
             JOIN fmapp.fm_data_uid fdu ON ((ffa.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
          WHERE ((ata.object_type)::text = 'BIO_DISEASE'::text)
          GROUP BY fdu.unique_id) diseases ON (((diseases.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM ((fmapp.fm_folder_association ffa
             JOIN fmapp.fm_data_uid fdu ON ((ffa.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
          WHERE ((ata.object_type)::text = 'BIO_COMPOUND'::text)
          GROUP BY fdu.unique_id) compounds ON (((compounds.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM (((fmapp.fm_folder_association ffa
             JOIN fmapp.fm_data_uid fdu ON ((ffa.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata.tag_item_id = ati.tag_item_id)))
          WHERE (((ata.object_type)::text = 'BIO_CONCEPT_CODE'::text) AND ((ati.code_type_name)::text = 'STUDY_OBJECTIVE'::text))
          GROUP BY fdu.unique_id) study_objectives ON (((study_objectives.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM (((fmapp.fm_folder_association ffa
             JOIN fmapp.fm_data_uid fdu ON ((ffa.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata.tag_item_id = ati.tag_item_id)))
          WHERE (((ata.object_type)::text = 'BIO_CONCEPT_CODE'::text) AND ((ati.code_type_name)::text = 'SPECIES'::text))
          GROUP BY fdu.unique_id) species ON (((species.id)::text = (fd.unique_id)::text)))
     LEFT JOIN ( SELECT fdu.unique_id AS id,
            string_agg((ata.object_uid)::text, '|'::text ORDER BY (ata.object_uid)::text) AS object_uids
           FROM (((fmapp.fm_folder_association ffa
             JOIN fmapp.fm_data_uid fdu ON ((ffa.folder_id = fdu.fm_data_id)))
             JOIN amapp.am_tag_association ata ON (((fdu.unique_id)::text = (ata.subject_uid)::text)))
             JOIN amapp.am_tag_item ati ON ((ata.tag_item_id = ati.tag_item_id)))
          WHERE (((ata.object_type)::text = 'BIO_CONCEPT_CODE'::text) AND ((ati.code_type_name)::text = 'STUDY_PHASE'::text))
          GROUP BY fdu.unique_id) phases ON (((phases.id)::text = (fd.unique_id)::text)))
  WHERE ff.active_ind;

--
-- Name: folder_study_mapping; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.folder_study_mapping AS
 WITH RECURSIVE study_nodes AS (
         SELECT i2b2_trial_nodes.c_fullname,
            ('EXP:'::text || i2b2_trial_nodes.trial) AS unique_id
           FROM i2b2metadata.i2b2_trial_nodes
        ), folders(folder_id, parent_id, unique_id) AS (
         SELECT f.folder_id,
            f.parent_id,
            fa.object_uid
           FROM (fmapp.fm_folder f
             LEFT JOIN fmapp.fm_folder_association fa ON ((f.folder_id = fa.folder_id)))
        ), map(folder_id, parent_id, unique_id, root) AS (
         SELECT folders.folder_id,
            folders.parent_id,
            folders.unique_id,
            true AS bool
           FROM folders
        UNION ALL
         SELECT map_1.folder_id,
            f.parent_id,
            f.unique_id,
            false AS bool
           FROM (map map_1
             JOIN folders f ON ((map_1.parent_id = f.folder_id)))
          WHERE ((map_1.parent_id IS NOT NULL) AND (map_1.unique_id IS NULL))
        )
 SELECT map.folder_id,
    map.unique_id,
    s.c_fullname,
    map.root
   FROM (map
     JOIN study_nodes s ON ((s.unique_id = upper((map.unique_id)::text))))
  WHERE (map.unique_id IS NOT NULL);

--
-- Name: patient_num_boundaries; Type: VIEW; Schema: biomart_user; Owner: -
--
CREATE VIEW biomart_user.patient_num_boundaries AS
WITH boundaries AS (
SELECT MIN(patient_num) AS min_patient_num, MAX(patient_num) as max_patient_num FROM i2b2demodata.patient_dimension
)
SELECT
  boundaries.min_patient_num,
  boundaries.max_patient_num,
  lpad('1', (boundaries.max_patient_num - boundaries.min_patient_num + 1)::integer, '0')::bit varying AS one
FROM boundaries;

--
-- Name: study_concept_bitset; Type: MATERIALIZED VIEW; Schema: biomart_user; Owner: -
--
CREATE MATERIALIZED VIEW biomart_user.study_concept_bitset AS
SELECT
  s.study_id AS study_id,
  o.concept_cd AS concept_cd,
  bit_or(patient_num_boundaries.one << (o.patient_num - patient_num_boundaries.min_patient_num)::INTEGER) AS patient_set_bits
FROM patient_num_boundaries, i2b2demodata.observation_fact o
JOIN i2b2demodata.trial_visit_dimension tv ON o.trial_visit_num = tv.trial_visit_num
JOIN i2b2demodata.study s ON s.study_num = tv.study_num
WHERE o.modifier_cd = '@'
GROUP BY s.study_id, o.concept_cd;

SET default_with_oids = false;

--
-- Name: subject_counts_per_study_and_concept(p_result_instance_id NUMERIC, p_study_ids VARCHAR[]); Type: FUNCTION; Schema: biomart_user; Owner: -
--
CREATE FUNCTION biomart_user.subject_counts_per_study_and_concept(p_result_instance_id NUMERIC, p_study_ids VARCHAR[]) RETURNS TABLE (
 study_id VARCHAR,
 concept_cd VARCHAR,
 patient_count INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN

  IF NOT EXISTS (SELECT 1 FROM biomart_user.patient_set_bitset WHERE result_instance_id = p_result_instance_id) THEN
    INSERT INTO biomart_user.patient_set_bitset SELECT
          collection.result_instance_id AS result_instance_id,
          (bit_or(patient_num_boundaries.one << (collection.patient_num - patient_num_boundaries.min_patient_num)::INTEGER)) AS patient_set
    FROM biomart_user.patient_num_boundaries, i2b2demodata.qt_patient_set_collection collection
    WHERE collection.result_instance_id = p_result_instance_id
    GROUP BY collection.result_instance_id;
  END IF;

  RETURN QUERY SELECT
      scs.study_id,
      scs.concept_cd,
      --less eficient length(replace((bitset_result)::text, '0', '')) could be used instead pg_bitcount ext. function.
      public.pg_bitcount(scs.patient_set_bits & psb.patient_set)
  FROM biomart_user.study_concept_bitset scs, biomart_user.patient_set_bitset psb
  WHERE psb.result_instance_id = p_result_instance_id AND scs.study_id = ANY(p_study_ids);
END;
$$;
