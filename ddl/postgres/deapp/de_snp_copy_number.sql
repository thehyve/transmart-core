--
-- Name: de_snp_copy_number; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_copy_number (
    patient_num bigint,
    trial_name character varying(20),
    snp_name character varying(50),
    chrom character varying(2),
    chrom_pos bigint,
    copy_number smallint
);

--
-- Name: de_snp_copy_number_num_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_snp_copy_number_num_idx ON de_snp_copy_number USING btree (patient_num);

