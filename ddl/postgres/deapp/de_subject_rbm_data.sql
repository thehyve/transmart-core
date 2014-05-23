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
    value double precision,
    log_intensity numeric,
    mean_intensity numeric,
    stddev_intensity numeric,
    median_intensity numeric,
    zscore double precision,
    rbm_panel character varying(50),
    unit character varying(50),
    id bigint NOT NULL
);

--
-- Name: pk_de_subject_rbm_data; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rbm_data
    ADD CONSTRAINT pk_de_subject_rbm_data PRIMARY KEY (id);
