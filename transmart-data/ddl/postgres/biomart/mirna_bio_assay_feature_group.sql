--
-- Name: seq_bio_data_id; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_bio_data_id
    START WITH 391713417
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: mirna_bio_assay_feature_group; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mirna_bio_assay_feature_group (
    bio_assay_feature_group_id numeric(18,0) DEFAULT nextval('seq_bio_data_id'::regclass) NOT NULL,
    feature_group_name character varying(100) NOT NULL,
    feature_group_type character varying(50) NOT NULL
);

--
-- Name: mirna_bio_assay_feature_group mirna_bio_asy_feature_grp_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mirna_bio_assay_feature_group
    ADD CONSTRAINT mirna_bio_asy_feature_grp_pk PRIMARY KEY (bio_assay_feature_group_id);

