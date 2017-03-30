--
-- Name: de_gene_info_gene_info_id_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_gene_info_gene_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_gene_info; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_gene_info (
    gene_info_id integer NOT NULL,
    gene_source_id integer DEFAULT 1 NOT NULL,
    entrez_id integer,
    gene_symbol character varying(255) NOT NULL,
    gene_name character varying(255),
    chrom character varying(40),
    chrom_start integer,
    chrom_stop integer,
    strand smallint
);

--
-- Name: de_gene_info gene_info_id; Type: DEFAULT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_gene_info ALTER COLUMN gene_info_id SET DEFAULT nextval('de_gene_info_gene_info_id_seq'::regclass);

--
-- Name: de_gene_info_entrez_id_gene_source_id_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_gene_info_entrez_id_gene_source_id_idx ON de_gene_info USING btree (entrez_id, gene_source_id);

--
-- Name: de_gene_info_gene_symbol_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_gene_info_gene_symbol_idx ON de_gene_info USING btree (gene_symbol);

