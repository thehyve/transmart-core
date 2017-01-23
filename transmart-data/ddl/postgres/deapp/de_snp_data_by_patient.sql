--
-- Name: de_snp_data_by_patient; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_data_by_patient (
    snp_data_by_patient_id bigint NOT NULL,
    snp_dataset_id bigint,
    trial_name character varying(255),
    patient_num bigint,
    chrom character varying(16),
    data_by_patient_chr text,
    ped_by_patient_chr text
);

--
-- Name: sys_c0020602; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_by_patient
    ADD CONSTRAINT sys_c0020602 PRIMARY KEY (snp_data_by_patient_id);

--
-- Name: tf_trg_snp_data_by_patient_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_snp_data_by_patient_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.SNP_DATA_BY_PATIENT_ID is null then
         select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_DATA_BY_PATIENT_ID ;
  end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_snp_data_by_patient_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_snp_data_by_patient_id BEFORE INSERT ON de_snp_data_by_patient FOR EACH ROW EXECUTE PROCEDURE tf_trg_snp_data_by_patient_id();

--
-- Name: fk_snp_dataset_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_by_patient
    ADD CONSTRAINT fk_snp_dataset_id FOREIGN KEY (snp_dataset_id) REFERENCES de_subject_snp_dataset(subject_snp_dataset_id);

