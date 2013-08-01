--
-- Name: sample_categories; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE sample_categories (
    trial_name character varying(100),
    tissue_type character varying(2000),
    data_types character varying(2000),
    disease character varying(2000),
    tissue_state character varying(2000),
    sample_id character varying(250),
    biobank character varying(3),
    source_organism character varying(255),
    treatment character varying(255),
    sample_treatment character varying(2000),
    subject_treatment character varying(2000),
    timepoint character varying(250)
);

