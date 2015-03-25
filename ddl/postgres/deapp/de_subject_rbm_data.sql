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
    id bigint NOT NULL,
    partition_id numeric
);

--
-- Name: pk_de_subject_rbm_data; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rbm_data
    ADD CONSTRAINT pk_de_subject_rbm_data PRIMARY KEY (id);

--
-- Name: tf_trg_de_subj_rbm_data_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_subj_rbm_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.ID is null then
 select nextval('deapp.DE_SUBJECT_RBM_DATA_SEQ') into NEW.ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_de_subj_rbm_data_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_subj_rbm_data_id BEFORE INSERT ON de_subject_rbm_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_subj_rbm_data_id();

--
-- Name: de_subject_rbm_data_assay_id_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_rbm_data
    ADD CONSTRAINT de_subject_rbm_data_assay_id_fk FOREIGN KEY (assay_id) REFERENCES de_subject_sample_mapping(assay_id) ON DELETE CASCADE;

--
-- Name: de_subject_rbm_data_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_subject_rbm_data_seq
    START WITH 1272564
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

