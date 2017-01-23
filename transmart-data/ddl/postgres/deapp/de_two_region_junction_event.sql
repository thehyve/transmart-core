--
-- Name: de_two_rgn_junction_event_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_two_rgn_junction_event_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_two_region_junction_event; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_two_region_junction_event (
    two_region_junction_event_id bigint DEFAULT nextval('de_two_rgn_junction_event_seq'::regclass) NOT NULL,
    junction_id bigint,
    event_id bigint,
    reads_span integer,
    reads_junction integer,
    pairs_span integer,
    pairs_junction integer,
    pairs_end integer,
    reads_counter integer,
    base_freq double precision
);

--
-- Name: COLUMN de_two_region_junction_event.reads_span; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.reads_span IS 'number of reads in the whole span';

--
-- Name: COLUMN de_two_region_junction_event.reads_junction; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.reads_junction IS 'number of reads spanning the junction';

--
-- Name: COLUMN de_two_region_junction_event.pairs_span; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.pairs_span IS 'number of spanning mate pairs ';

--
-- Name: COLUMN de_two_region_junction_event.pairs_junction; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.pairs_junction IS 'number of spanning mate pairs where one end spans a fusion ';

--
-- Name: COLUMN de_two_region_junction_event.pairs_end; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.pairs_end IS 'number of mate pairs that support the fusion and whose one end spans the fusion';

--
-- Name: COLUMN de_two_region_junction_event.reads_counter; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.reads_counter IS 'number of reads that contradict the fusion by mapping to only one of the chromosomes';

--
-- Name: COLUMN de_two_region_junction_event.base_freq; Type: COMMENT; Schema: deapp; Owner: -
--
COMMENT ON COLUMN de_two_region_junction_event.base_freq IS 'frequency in the baseline set of genomes for the junction';

--
-- Name: two_region_junction_event_id_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_junction_event
    ADD CONSTRAINT two_region_junction_event_id_pk PRIMARY KEY (two_region_junction_event_id);

--
-- Name: two_region_junction_event_id_event_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_junction_event
    ADD CONSTRAINT two_region_junction_event_id_event_fk FOREIGN KEY (event_id) REFERENCES de_two_region_event(two_region_event_id);

--
-- Name: two_region_junction_event_id_junction_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_two_region_junction_event
    ADD CONSTRAINT two_region_junction_event_id_junction_fk FOREIGN KEY (junction_id) REFERENCES de_two_region_junction(two_region_junction_id);

