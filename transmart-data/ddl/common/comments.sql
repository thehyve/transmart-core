--
-- deapp.de_gpl_info
--
COMMENT ON TABLE deapp.de_gpl_info IS 'Definition of GPL platforms';

COMMENT ON COLUMN de_gpl_info.platform IS 'Primary key. Platform id. E.g., GPL1000, GPL96, RNASEQ_TRANSCRIPT_PLATFORM.';
COMMENT ON COLUMN de_gpl_info.title IS 'Title of the platform. E.g., microarray test data, rnaseq transcript level test data.';
COMMENT ON COLUMN de_gpl_info.organism IS 'Organism the platform applies to. E.g., Human.';
COMMENT ON COLUMN de_gpl_info.marker_type IS 'E.g., Gene Expression, RNASEQ_TRANSCRIPT.';
COMMENT ON COLUMN de_gpl_info.genome_build IS 'E.g., hg19.';

--
-- deapp.de_rnaseq_transcript_data
--
COMMENT ON TABLE deapp.de_rnaseq_transcript_data IS 'Table holds rnaseq transcript level values.';

COMMENT ON COLUMN de_rnaseq_transcript_data.transcript_id IS 'Id of the transcript, linking to id in de_rnaseq_transcript_annot.';
COMMENT ON COLUMN de_rnaseq_transcript_data.assay_id IS 'Primary key. Id used to link highdim data to assays in the de_subject_sample_mapping table.';
COMMENT ON COLUMN de_rnaseq_transcript_data.readcount IS 'Base count.';
COMMENT ON COLUMN de_rnaseq_transcript_data.normalized_readcount IS 'Normalized projection.';
COMMENT ON COLUMN de_rnaseq_transcript_data.log_normalized_readcount IS 'Log projection.';
COMMENT ON COLUMN de_rnaseq_transcript_data.zscore IS 'Zscore projection.';