--
-- Name: de_snp_data_by_probe; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_data_by_probe (
    snp_data_by_probe_id bigint NOT NULL,
    probe_id bigint,
    probe_name character varying(255),
    snp_id bigint,
    snp_name character varying(255),
    trial_name character varying(255),
    data_by_probe text,
    bio_asy_geno_platform_probe_id bigint,
    genotype_probe_annotation_id bigint,
    a1 character varying(4000),
    a2 character varying(4000),
    a1_clob text,
    a2_clob text,
    impute_quality numeric,
    gps_by_probe_blob text,
    gts_by_probe_blob text,
    dose_by_probe_blob text,
    gt_probability_threshold numeric,
    maf numeric,
    minor_allele character varying(2),
    c_a1_a1 numeric,
    c_a1_a2 numeric,
    c_a2_a2 numeric,
    c_nocall numeric,
    created_by character varying(30),
    created_date date,
    modified_by character varying(30),
    modified_date date
);

--
-- Name: de_snp_data_by_probe sys_c0020601; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_by_probe
    ADD CONSTRAINT sys_c0020601 PRIMARY KEY (snp_data_by_probe_id);

--
-- Name: idx_dsdbp_probe_idname; Type: INDEX; Schema: deapp; Owner: -
--
CREATE UNIQUE INDEX idx_dsdbp_probe_idname ON de_snp_data_by_probe USING btree (snp_data_by_probe_id, probe_name);

--
-- Name: idx_dsdbp_trial_name; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX idx_dsdbp_trial_name ON de_snp_data_by_probe USING btree (trial_name);

--
-- Name: fun_snp_data_by_pprobe_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION fun_snp_data_by_pprobe_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
      IF NEW.SNP_DATA_BY_PROBE_ID IS NULL
      THEN
         SELECT NEXTVAL('DEAPP.SEQ_DATA_ID') INTO NEW.SNP_DATA_BY_PROBE_ID;
      END IF;
END;
$$;

--
-- Name: de_snp_data_by_probe trg_snp_data_by_pprobe_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_snp_data_by_pprobe_id BEFORE INSERT ON de_snp_data_by_probe FOR EACH ROW EXECUTE PROCEDURE fun_snp_data_by_pprobe_id();

--
-- Name: tf_trg_snp_data_by_probe_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_snp_data_by_probe_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.SNP_DATA_BY_PROBE_ID is null then
         select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_DATA_BY_PROBE_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: de_snp_data_by_probe trg_snp_data_by_probe_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_snp_data_by_probe_id BEFORE INSERT ON de_snp_data_by_probe FOR EACH ROW EXECUTE PROCEDURE tf_trg_snp_data_by_probe_id();

--
-- Name: de_snp_data_by_probe fk_snp_by_probe_probe_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_by_probe
    ADD CONSTRAINT fk_snp_by_probe_probe_id FOREIGN KEY (probe_id) REFERENCES de_snp_probe(snp_probe_id);

--
-- Name: de_snp_data_by_probe fk_snp_by_probe_snp_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_data_by_probe
    ADD CONSTRAINT fk_snp_by_probe_snp_id FOREIGN KEY (snp_id) REFERENCES de_snp_info(snp_info_id);

--
-- Name: seq_data_id; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE seq_data_id
    START WITH 11594011
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

