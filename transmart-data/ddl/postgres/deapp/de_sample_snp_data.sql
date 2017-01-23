--
-- Name: de_sample_snp_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_sample_snp_data (
    sample_id character varying(50),
    snp_name character varying(100),
    snp_calls character varying(4),
    copy_number double precision
);

--
-- Name: idx_de_sample_snp_data_sample; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX idx_de_sample_snp_data_sample ON de_sample_snp_data USING btree (sample_id);

--
-- Name: idx_de_sample_snp_data_snp; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX idx_de_sample_snp_data_snp ON de_sample_snp_data USING btree (snp_name);

