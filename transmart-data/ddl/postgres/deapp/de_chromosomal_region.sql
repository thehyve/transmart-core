--
-- Name: de_chromo_region_id_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_chromo_region_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_chromosomal_region; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_chromosomal_region (
    region_id bigint DEFAULT nextval('de_chromo_region_id_seq'::regclass) NOT NULL,
    gpl_id character varying(50),
    chromosome character varying(2),
    start_bp bigint,
    end_bp bigint,
    num_probes integer,
    region_name character varying(100),
    cytoband character varying(100),
    gene_symbol character varying(100),
    gene_id bigint,
    organism character varying(200)
);

--
-- Name: de_chromosomal_region_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_chromosomal_region
    ADD CONSTRAINT de_chromosomal_region_pkey PRIMARY KEY (region_id);

--
-- Name: de_chrom_region_gpl_id_fkey; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_chromosomal_region
    ADD CONSTRAINT de_chrom_region_gpl_id_fkey FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform);

