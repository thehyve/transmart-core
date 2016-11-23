--
-- Name: de_two_region_event_gene_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_two_region_event_gene_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_two_region_event_gene; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_two_region_event_gene (
    two_region_event_gene_id bigint DEFAULT nextval('de_two_region_event_gene_seq'::regclass) NOT NULL,
    gene_id character varying(50) NOT NULL,
    event_id bigint NOT NULL,
    effect character varying(500)
);

--
-- Name: COLUMN de_two_region_event_gene.gene_id; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_event_gene.gene_id IS 'HUGO gene identifier';

--
-- Name: COLUMN de_two_region_event_gene.effect; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_event_gene.effect IS 'effect of the event on the gene: FUSION, CONTAINED, DISRUPTED, ...';

--
-- Name: two_region_event_gene_id_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_event_gene
    ADD CONSTRAINT two_region_event_gene_id_pk PRIMARY KEY (two_region_event_gene_id);

--
-- Name: two_region_event_gene_id_event_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_event_gene
    ADD CONSTRAINT two_region_event_gene_id_event_fk FOREIGN KEY (event_id) REFERENCES de_two_region_event(two_region_event_id);

