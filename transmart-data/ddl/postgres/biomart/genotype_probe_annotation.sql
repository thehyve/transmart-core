--
-- Name: genotype_probe_annotation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE genotype_probe_annotation (
    genotype_probe_annotation_id numeric(22,0) NOT NULL,
    snp_name character varying(50),
    chrom character varying(4),
    pos numeric,
    ref character varying(4000),
    alt character varying(4000),
    gene_info character varying(4000),
    variation_class character varying(10),
    strand boolean,
    exon_intron character varying(30),
    genome_build character varying(10),
    snp_source character varying(10),
    recombination_rate numeric(18,6),
    recombination_map numeric(18,6),
    regulome_score character varying(10),
    ref_clob text,
    alt_clob text,
    created_by character varying(30),
    created_date date,
    modified_by character varying(30),
    modified_date date
);

--
-- Name: idx_geno_anno_loc; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_geno_anno_loc ON genotype_probe_annotation USING btree (chrom, pos);

--
-- Name: idx_probe_anno_snp; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX idx_probe_anno_snp ON genotype_probe_annotation USING btree (snp_name);

--
-- Name: pk_geno_probe_annotation; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX pk_geno_probe_annotation ON genotype_probe_annotation USING btree (genotype_probe_annotation_id);

--
-- Name: fun_annotation_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION fun_annotation_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
      IF NEW.GENOTYPE_PROBE_ANNOTATION_ID IS NULL
      THEN
         SELECT NEXTVAL('biomart.seq_annotation_id')
           INTO NEW.GENOTYPE_PROBE_ANNOTATION_ID;
      END IF;

END;
$$;

--
-- Name: genotype_probe_annotation trg_annotation_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_annotation_id BEFORE INSERT ON genotype_probe_annotation FOR EACH ROW EXECUTE PROCEDURE fun_annotation_id();

--
-- Name: seq_annotation_id; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_annotation_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

