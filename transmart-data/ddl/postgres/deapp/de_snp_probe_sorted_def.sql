--
-- Name: de_snp_probe_sorted_def; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_probe_sorted_def (
    snp_probe_sorted_def_id bigint NOT NULL,
    platform_name character varying(255),
    num_probe bigint,
    chrom character varying(16),
    probe_def text,
    snp_id_def text
);

--
-- Name: de_snp_probe_sorted_def sys_c0020600; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_probe_sorted_def
    ADD CONSTRAINT sys_c0020600 PRIMARY KEY (snp_probe_sorted_def_id);

--
-- Name: tf_trg_de_snp_probe_sorted_def_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_snp_probe_sorted_def_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.SNP_PROBE_SORTED_DEF_ID is null then
         select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_PROBE_SORTED_DEF_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: de_snp_probe_sorted_def trg_de_snp_probe_sorted_def_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_snp_probe_sorted_def_id BEFORE INSERT ON de_snp_probe_sorted_def FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_snp_probe_sorted_def_id();

