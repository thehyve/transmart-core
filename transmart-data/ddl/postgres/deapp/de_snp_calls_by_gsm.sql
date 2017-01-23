--
-- Name: de_snp_calls_by_gsm; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_calls_by_gsm (
    gsm_num character varying(100),
    trial_name character varying(20),
    patient_num bigint,
    snp_name character varying(100),
    snp_calls character varying(4)
);

--
-- Name: de_snp_calls_by_gsm_patient_num_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_snp_calls_by_gsm_patient_num_idx ON de_snp_calls_by_gsm USING btree (patient_num);

