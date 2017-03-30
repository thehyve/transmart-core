--
-- Name: de_snp_data_dataset_loc; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_data_dataset_loc (
    snp_data_dataset_loc_id bigint NOT NULL,
    trial_name character varying(255),
    snp_dataset_id bigint,
    location bigint
);

--
-- Name: de_snp_data_dataset_loc de_snp_data_dataset_loc_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_dataset_loc
    ADD CONSTRAINT de_snp_data_dataset_loc_pkey PRIMARY KEY (snp_data_dataset_loc_id);

--
-- Name: tf_trg_snp_data_dataset_loc_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_snp_data_dataset_loc_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.SNP_DATA_DATASET_LOC_ID is null then
 select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_DATA_DATASET_LOC_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: de_snp_data_dataset_loc trg_snp_data_dataset_loc_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_snp_data_dataset_loc_id BEFORE INSERT ON de_snp_data_dataset_loc FOR EACH ROW EXECUTE PROCEDURE tf_trg_snp_data_dataset_loc_id();

--
-- Name: de_snp_data_dataset_loc fk_snp_loc_dataset_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_dataset_loc
    ADD CONSTRAINT fk_snp_loc_dataset_id FOREIGN KEY (snp_dataset_id) REFERENCES de_subject_snp_dataset(subject_snp_dataset_id);

