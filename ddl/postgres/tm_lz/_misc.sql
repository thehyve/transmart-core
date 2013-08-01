--
-- Name: lt_chromosomal_region_region_id_seq; Type: SEQUENCE; Schema: tm_lz; Owner: -
--
CREATE SEQUENCE lt_chromosomal_region_region_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: lt_chromosomal_region_region_id_seq; Type: SEQUENCE OWNED BY; Schema: tm_lz; Owner: -
--
ALTER SEQUENCE lt_chromosomal_region_region_id_seq OWNED BY lt_chromosomal_region.region_id;

--
-- Name: region_id; Type: DEFAULT; Schema: tm_lz; Owner: -
--
ALTER TABLE ONLY lt_chromosomal_region ALTER COLUMN region_id SET DEFAULT nextval('lt_chromosomal_region_region_id_seq'::regclass);

--
-- Name: lt_chromosomal_region_gpl_id_fkey; Type: FK CONSTRAINT; Schema: tm_lz; Owner: -
--
ALTER TABLE ONLY lt_chromosomal_region
    ADD CONSTRAINT lt_chromosomal_region_gpl_id_fkey FOREIGN KEY (gpl_id) REFERENCES deapp.de_gpl_info(platform);

