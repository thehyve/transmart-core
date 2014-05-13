-- Type: SEQUENCE; Owner: DEAPP; Name: DE_SUBJECT_RBM_DATA_SEQ
--
CREATE SEQUENCE de_subject_rbm_data_seq
    NO MINVALUE
    NO MAXVALUE
    INCREMENT BY 1
    START WITH 61
    CACHE 1
;

--
-- Name: de_subject_rbm_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_rbm_data (
    trial_name character varying(100),
    antigen_name character varying(100),
    n_value bigint,
    patient_id bigint,
    gene_symbol character varying(100),
    gene_id integer,
    assay_id bigint,
    normalized_value double precision,
    concept_cd character varying(100),
    timepoint character varying(100),
    data_uid character varying(100),
    value bigint,
    log_intensity bigint,
    mean_intensity bigint,
    stddev_intensity bigint,
    median_intensity bigint,
    zscore bigint,
    rbm_panel character varying(50),
    unit character varying(50),
    id bigint
);


--
-- Name: pk_de_subject_rbm_data; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rbm_data
    ADD CONSTRAINT pk_de_subject_rbm_data PRIMARY KEY (id);
