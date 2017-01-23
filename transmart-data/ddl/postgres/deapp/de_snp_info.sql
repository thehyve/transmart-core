--
-- Name: de_snp_info; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_info (
    snp_info_id bigint NOT NULL,
    name character varying(255),
    chrom character varying(16),
    chrom_pos bigint
);

--
-- Name: de_snp_info_id_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_info
    ADD CONSTRAINT de_snp_info_id_pk PRIMARY KEY (snp_info_id);

--
-- Name: u_snp_info_name; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_info
    ADD CONSTRAINT u_snp_info_name UNIQUE (name);

--
-- Name: tf_trg_de_snp_info_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_snp_info_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.SNP_INFO_ID is null then
         select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_INFO_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_de_snp_info_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_snp_info_id BEFORE INSERT ON de_snp_info FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_snp_info_id();

