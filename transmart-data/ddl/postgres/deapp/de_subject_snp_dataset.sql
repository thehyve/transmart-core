--
-- Name: de_subject_snp_dataset; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_snp_dataset (
    subject_snp_dataset_id bigint NOT NULL,
    dataset_name character varying(255),
    concept_cd character varying(255),
    platform_name character varying(255),
    trial_name character varying(255),
    patient_num bigint,
    timepoint character varying(255),
    subject_id character varying(255),
    sample_type character varying(255),
    paired_dataset_id bigint,
    patient_gender character varying(1)
);

--
-- Name: de_subject_snp_dataset sys_c0020606; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_subject_snp_dataset
    ADD CONSTRAINT sys_c0020606 PRIMARY KEY (subject_snp_dataset_id);

--
-- Name: tf_trg_de_subject_snp_dataset_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_subject_snp_dataset_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.SUBJECT_SNP_DATASET_ID is null then
         select nextval('deapp.SEQ_DATA_ID') into NEW.SUBJECT_SNP_DATASET_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: de_subject_snp_dataset trg_de_subject_snp_dataset_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_subject_snp_dataset_id BEFORE INSERT ON de_subject_snp_dataset FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_subject_snp_dataset_id();

