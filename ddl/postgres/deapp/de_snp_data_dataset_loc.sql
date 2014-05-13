--
-- Name: de_snp_data_dataset_loc; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_data_dataset_loc (
    snp_data_dataset_loc_id bigint,
    trial_name character varying(255),
    snp_dataset_id bigint,
    location bigint,
    PRIMARY KEY (snp_data_dataset_loc_id)
);

--
-- Name: fk_snp_loc_dataset_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_dataset_loc
    ADD CONSTRAINT fk_snp_loc_dataset_id FOREIGN KEY (snp_dataset_id) REFERENCES de_subject_snp_dataset(subject_snp_dataset_id);

