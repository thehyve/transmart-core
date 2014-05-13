--
-- Name: mirna_bio_assay_data_annot; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mirna_bio_assay_data_annot (
    bio_assay_feature_group_id bigint,
    bio_marker_id bigint NOT NULL,
    data_table character(5)
);
