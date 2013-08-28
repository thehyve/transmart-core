--
-- Name: bio_marker_exp_analysis_mv; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_marker_exp_analysis_mv AS
 SELECT DISTINCT t3.bio_marker_id, 
    t1.bio_experiment_id, 
    t1.bio_assay_analysis_id, 
    ((t1.bio_assay_analysis_id * 100) + t3.bio_marker_id) AS mv_id
   FROM bio_assay_analysis_data t1, 
    bio_experiment t2, 
    bio_marker t3, 
    bio_assay_data_annotation t4
  WHERE ((((t1.bio_experiment_id = t2.bio_experiment_id) AND ((t2.bio_experiment_type)::text = 'Experiment'::text)) AND (t3.bio_marker_id = t4.bio_marker_id)) AND (t1.bio_assay_feature_group_id = t4.bio_assay_feature_group_id));

